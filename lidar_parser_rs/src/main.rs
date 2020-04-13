mod parser;

use anyhow::Result;
use clap::{App, Arg};
use postgres::NoTls;
use r2d2_postgres::PostgresConnectionManager;

fn main() -> Result<()> {
    let matches = App::new("Lidar Parser")
        .version("1.0")
        .about("Uploads lidar data from .bag files as serialized float arrays.")
        .arg(
            Arg::with_name("title")
                .short("t")
                .long("title")
                .value_name("TITLE")
                .help("Sets the title for the new recording")
                .default_value("rust_data")
                .takes_value(true),
        )
        .arg(
            Arg::with_name("ip_address")
                .short("i")
                .long("ip")
                .value_name("IPADDRESS")
                .help("Set the ip address of the database server")
                .default_value("localhost")
                .takes_value(true),
        )
        .arg(
            Arg::with_name("user")
                .short("u")
                .long("user")
                .value_name("USER")
                .help("Set the user as whom to login")
                .default_value("lidar")
                .takes_value(true),
        )
        .arg(
            Arg::with_name("password")
                .short("p")
                .long("password")
                .value_name("PASSWORD")
                .help("Set the password for the user")
                .default_value("mindhash")
                .takes_value(true),
        )
        .arg(
            Arg::with_name("dbname")
                .short("n")
                .long("dbname")
                .value_name("DBNAME")
                .help("Set the name of the database to connect to")
                .default_value("lidar")
                .takes_value(true),
        )
        .arg(Arg::with_name("file").required(true))
        .get_matches();

    let file_path = matches.value_of("file").unwrap();
    let ip_address = matches.value_of("ip_address").unwrap();
    let dbname = matches.value_of("dbname").unwrap();
    let user_name = matches.value_of("user").unwrap();
    let title = matches.value_of("title").unwrap();

    println!("Parsing '{}'", file_path);
    println!("Connecting to '{}@{}/{}'", user_name, ip_address, dbname);
    let manager = PostgresConnectionManager::new(
        format!(
            "host={} user={} password={} dbname={}",
            ip_address,
            user_name,
            matches.value_of("password").unwrap(),
            dbname
        )
        .parse()
        .unwrap(),
        NoTls,
    );
    let pool = r2d2::Pool::new(manager).unwrap();

    println!("Connected to database");
    println!("Parsing and uploading contents now");
    parser::parse_bag(file_path, &pool, &title)?;
    println!("Upload complete");

    Ok(())
}
