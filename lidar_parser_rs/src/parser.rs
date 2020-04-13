use anyhow::Result;
use byteorder::{BigEndian, LittleEndian, ReadBytesExt, WriteBytesExt};
use r2d2;
use r2d2_postgres;
use rosbag::{Record, RosBag};
use std::collections::HashMap;
use std::f32::consts;
use std::io::Cursor;
use std::path::Path;
// use std::slice;
use threadpool::ThreadPool;

const BEAM_AZIMUTH_ANGLES: [f32; 64] = [
    3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
    3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
    3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
    3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
    3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
    3.164, 1.055, -1.055, -3.164,
];

const BEAM_ALTITUDE_ANGLES: [f32; 64] = [
    16.611, 16.084, 15.557, 15.029, 14.502, 13.975, 13.447, 12.920, 12.393, 11.865, 11.338, 10.811,
    10.283, 9.756, 9.229, 8.701, 8.174, 7.646, 7.119, 6.592, 6.064, 5.537, 5.010, 4.482, 3.955,
    3.428, 2.900, 2.373, 1.846, 1.318, 0.791, 0.264, -0.264, -0.791, -1.318, -1.846, -2.373,
    -2.900, -3.428, -3.955, -4.482, -5.010, -5.537, -6.064, -6.592, -7.119, -7.646, -8.174, -8.701,
    -9.229, -9.756, -10.283, -10.811, -11.338, -11.865, -12.393, -12.920, -13.447, -13.975,
    -14.502, -15.029, -15.557, -16.084, -16.611,
];

#[derive(Debug)]
pub struct DataBlock {
    range: u32,
    signal_photons: u16,
    reflectivity: u16,
    ambient_noise_photons: u16,
}

#[derive(Debug)]
pub struct AzimuthBlock {
    timestamp: u64,
    measurement_id: u16,
    frame_id: u16,
    encoder_count: u32,
    data_blocks: Vec<DataBlock>,
    block_status: bool,
}

#[derive(Debug)]
pub struct LidarPacket {
    blocks: Vec<AzimuthBlock>,
}

#[derive(Debug)]
pub struct Frame {
    pub id: u16,
    pub points: Vec<f32>,
}

fn read_packet(data: &[u8]) -> Result<LidarPacket> {
    let mut rdr = Cursor::new(data);
    let mut blocks = Vec::with_capacity(16);
    // Header of field
    rdr.set_position(4);

    for _ in 0..16 {
        blocks.push(read_azimuth_block(&mut rdr)?);
    }

    Ok(LidarPacket { blocks })
}

fn read_azimuth_block(rdr: &mut Cursor<&[u8]>) -> Result<AzimuthBlock> {
    let timestamp = rdr.read_u64::<LittleEndian>()?;
    let measurement_id = rdr.read_u16::<LittleEndian>()?;
    let frame_id = rdr.read_u16::<LittleEndian>()?;
    let encoder_count = rdr.read_u32::<LittleEndian>()?;

    let mut data_blocks = Vec::with_capacity(64);
    for _ in 0..64 {
        let range = rdr.read_u32::<LittleEndian>()? & 0xFFFFF;
        let signal_photons = rdr.read_u16::<LittleEndian>()?;
        let reflectivity = rdr.read_u16::<LittleEndian>()?;
        rdr.read_u16::<LittleEndian>()?; // Skip 16 bits
        let ambient_noise_photons = rdr.read_u16::<LittleEndian>()?;
        data_blocks.push(DataBlock {
            range,
            signal_photons,
            reflectivity,
            ambient_noise_photons,
        })
    }

    let block_status = rdr.read_u32::<LittleEndian>()? == 0xFFFFFFFF;

    let block = AzimuthBlock {
        timestamp,
        measurement_id,
        frame_id,
        encoder_count,
        data_blocks,
        block_status,
    };
    Ok(block)
}

pub fn azimuth_block_to_coords(block: &AzimuthBlock) -> Vec<f32> {
    let mut v = Vec::with_capacity(64 * 3);
    block.data_blocks.iter().enumerate().for_each(|(i, b)| {
        let r = b.range as f32 / 1000f32;
        let theta = 2f32
            * consts::PI
            * (block.encoder_count as f32 / 90112f32 + BEAM_AZIMUTH_ANGLES[i] / 360f32);
        let phi = 2f32 * consts::PI * (BEAM_ALTITUDE_ANGLES[i] / 360f32);
        let x = r * theta.cos() * phi.cos();
        let y = -r * theta.sin() * phi.cos();
        let z = r * phi.sin();
        if x.is_normal() || y.is_normal() || z.is_normal() {
            v.push(x);
            v.push(y);
            v.push(z);
        }
    });
    v
}

pub fn parse_bag<P: AsRef<Path>>(
    path: P,
    conn_pool: &r2d2::Pool<r2d2_postgres::PostgresConnectionManager<postgres::NoTls>>,
    title: &str,
) -> Result<()> {
    let bag = RosBag::new(path)?;
    let mut records = bag.records();

    let mut frames: HashMap<u16, Vec<f32>> = HashMap::new();
    let mut last_frame_id: Option<u16> = None;
    let thread_pool = ThreadPool::new(8);

    let mut client = conn_pool
        .clone()
        .get()
        .expect("Could not get db connection");
    let recording_id: i32 = client
        .query(
            "INSERT INTO recording (title) VALUES ($1) RETURNING id;",
            &[&title],
        )?
        .first()
        .expect("Recording insert failed.")
        .get(0);

    let mut max_points = 0;
    let mut min_frame_id = std::i32::MAX;
    let mut max_frame_id = std::i32::MIN;

    for record in &mut records {
        if let Record::Chunk(chunk) = record? {
            for msg in chunk.iter_msgs() {
                let m = msg?;
                if m.data.len() > 12000 as usize {
                    let packet = read_packet(&m.data)?;
                    // Assume that all blocks in a packet have the same frame id
                    let frame_id = packet
                        .blocks
                        .first()
                        .expect("No blocks in packet.")
                        .frame_id;
                    let v = frames
                        .entry(frame_id)
                        .or_insert(Vec::with_capacity(50000 * 3));
                    packet
                        .blocks
                        .iter()
                        .map(azimuth_block_to_coords)
                        .for_each(|ref mut coords| {
                            v.append(coords);
                        });
                    match last_frame_id {
                        Some(last_id) => {
                            // If the current frame id is bigger than the previous one then
                            // insert the frame under the assumption that the data in the file
                            // is ordered.
                            if last_id < frame_id {
                                last_frame_id = Some(frame_id);
                                if let Some((k, v)) = frames.remove_entry(&last_id) {
                                    // Insert into database
                                    let conn_pool = conn_pool.clone();

                                    // Update recording meta data
                                    if v.len() / 3 > max_points {
                                        max_points = v.len() / 3;
                                    }
                                    if (k as i32) < min_frame_id {
                                        min_frame_id = k as i32;
                                    }
                                    if (k as i32) > max_frame_id {
                                        max_frame_id = k as i32;
                                    }

                                    // Update last frame id because a new frame is being built
                                    // now
                                    thread_pool.execute(move|| {
                                        let mut conn =
                                            conn_pool.get().expect("Could not get connection");
                                        let mut raw_points: Vec<u8> = Vec::with_capacity(v.len() * 4);
                                        for p in v {
                                            raw_points.write_f32::<BigEndian>(p).expect("Could not write points");
                                        }
                                        conn.execute(
                                            "INSERT INTO frame (frameid, recid, points) VALUES ($1, $2, $3);",
                                            &[&(k as i32), &recording_id, &raw_points],
                                        ).expect("Query failed");
                                        /*
                                        unsafe {
                                            let raw = slice::from_raw_parts(v.as_ptr() as *const u8, v.len() * 4);
                                            conn.execute(
                                                "INSERT INTO frame (frameid, recid, points) VALUES ($1, $2, $3);",
                                                &[&(k as i32), &recording_id, &raw],
                                            ).expect("Query failed");
                                        }
                                        */
                                    });
                                };
                            }
                        }
                        None => last_frame_id = Some(frame_id),
                    }
                }
            }
        }
    }

    println!("All data parsed, waiting for inserts to complete");
    thread_pool.join();

    // Update recording info
    println!("All inserts complete, updating recording meta data");
    client.execute(
        "UPDATE recording SET minframe = $1, maxframe = $2, maxpoints = $3 WHERE id = $4",
        &[
            &min_frame_id,
            &max_frame_id,
            &(max_points as i32),
            &recording_id,
        ],
    )?;

    Ok(())
}
