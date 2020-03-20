import org.quokka.kotlin.internals.LidarFrame


//----------Add object with material and texture---------



//spaceObjects = ArrayList<ModelInstance>(1)
//var spaceObjects: ArrayList<ModelInstance>? = null
//var instance: ModelInstance? = null
//
//var bottomBlock: Model? = null
//
//var pink: Texture? = null
//blueYellowFade = Array(256) { i ->
//    val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
//    pix.setColor(i / 255f, i / 255f, 1 - i / 255f, 1f)
//    pix.drawPixel(0, 0)
//    TextureRegion(Texture(pix))
//}
//
//
//modelBuilder.begin()
//modelBuilder.node().id = "Floor"
//pink = Texture(Gdx.files.internal("core/assets/badlogic.jpg"), false)
//pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
//pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
//var material = Material(TextureAttribute.createDiffuse(pink))
//modelBuilder.end()
//
//bottomBlock = modelBuilder.createBox(
//10f, 10f, .5f,
//material,
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)



//package org.quokka.kotlin.Enviroment
//
//import org.quokka.kotlin.internals.LidarReader
//import com.badlogic.gdx.graphics.g3d.ModelInstance
//
//fun populate(){
//    spaceObjects= ArrayList(1001)
//
//    instance = ModelInstance(bottomBlock,0f,0f,0f)
//    spaceObjects!!.add(instance!!)
//    for(i in -25..25){
//        for (j in -25..25){
////                for (k in -25..25) {
//            instance = ModelInstance(proxi, i * 1F, j * 1F, 1F)
//            spaceObjects!!.add(instance!!)
////                }
//        }
////            instance = ModelInstance(proxi,randx+offzet+i,randy+i,10f)
////            array!!.add(instance!!)
//    }
////        if(randx + offzet>50){
////            offzet = 0
////        }
////        offzet++
//
//}
//
//
//
//
//fun filepopp(){
//    spaceObjects= ArrayList(53000)
//    val ldrrdr =  LidarReader.DefaultReader()
//    var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag",2000,2010)
//
//
//    intermetidate.first().coords.forEach { f ->
//        instance = ModelInstance(proxi,
//                1f * f.coords.first,
//                1f * f.coords.second,
//                1f * f.coords.third)
//        spaceObjects!!.add(instance!!)
//    }
//}
//
//
//fun randpop(){
//    spaceObjects = ArrayList(53000)
//    instance = ModelInstance(bottomBlock,0f,0f,0f)
//    spaceObjects!!.add(instance!!)
////        for (i in 0..53000){
////            var rand = (0..53000).shuffled()
////            instance = ModelInstance(proxi,randx.get(i)*1F,randy.get(i)*1F,randz.get(i)*1F)
////            array!!.add(instance!!)
////        }
//    for (i in -50..50)
//        for(j in -25..25)
//            for (k in -5..5) {
//                instance = ModelInstance(proxi, i * 1f, j * 1f, k * 1f)
//                spaceObjects!!.add(instance!!)
//            }




fun getMaxCoord(frame: LidarFrame):Triple<Int,Int,Int>{
    var x = 0
    var y = 0
    var z = 0

    for (i in frame.coords){
        if(i.x > x)
            x = i.x.toInt()
        if(i.y > y)
            y = i.y.toInt()
        if(i.z > z)
            z = i.z.toInt()
    }
    return Triple(x+1,y+1,z+1)
}

fun getMinCoord(frame: LidarFrame):Triple<Int,Int,Int>{
    var x = 0
    var y = 0
    var z = 0

    for (i in frame.coords){
        if(i.x < x)
            x = i.x.toInt()
        if(i.y < y)
            y = i.y.toInt()
        if(i.z < z)
            z = i.z.toInt()
    }
    return Triple(x-1,y-1,z-1)
}



//proxi = modelBuilder.createBox(
//10f, 10f, 10f,
//Material(ColorAttribute.createDiffuse(Color.GREEN)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//
//onethreePoint = modelBuilder.createBox(boxsize,boxsize,boxsize,
//Material(ColorAttribute.createDiffuse(Color.LIME)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//
//foursixPoint = modelBuilder.createBox(boxsize,boxsize,boxsize,
//Material(ColorAttribute.createDiffuse(Color.YELLOW)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//
//sevenninePoint = modelBuilder.createBox(
//boxsize,boxsize,boxsize,
//Material(ColorAttribute.createDiffuse(Color.ORANGE)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//tentwelvePoint = modelBuilder.createBox(
//boxsize,boxsize,boxsize,
//Material(ColorAttribute.createDiffuse(Color.BLUE)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//thriteenfifteenPoint = modelBuilder.createBox(
//boxsize,boxsize,boxsize,
//Material(ColorAttribute.createDiffuse(Color.PINK)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)
//
////        fifteeneighteenPoint = modelBuilder.createBox(
////               .35f, .35f, .35f,
////                Material(ColorAttribute.createDiffuse(Color.GOLD)),
////                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
////        )
//
//morePoint = modelBuilder.createBox(
//boxsize+.2f,boxsize+.2f,boxsize+.2f,
//Material(ColorAttribute.createDiffuse(Color.RED)),
//(VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//)

//




//fun shave():ArrayList<ModelInstance>{
//    var objects = ArrayList<ModelInstance>(15)
//    var map = HashMap<Triple<Float,Float,Float>,Int>()
//
////        if(frames!!.isEmpty()){
////            objects.add(ModelInstance(proxi,0f,0f,0f))
////            println("empty frame")
////            return objects
////        }
//
//    while(frames!!.isEmpty() || frames!!.peek().coords.size < 1000){
//        frames!!.poll()
//    }
//
//    var crtFrame = frames!!.poll()
//    crtFrame.coords.forEach { c ->
//
//        val divisions = decidDivisions(c)
//
//        val tripp
//                = Triple(
//                decideCPR(c.x,divisions),
//                decideCPR(c.y,divisions),
//                decideCPR(c.z,divisions))
//
//        if(map.keys.contains(tripp)){
//            map.set(tripp,map.getValue(tripp)+1)
//        } else {
//            map.set(tripp,1)
//        }
//    }
//
//    val margin = 5
//    for (key in map.keys){
//        if(map.get(key) in 1..margin){
//            objects.add(ModelInstance(onethreePoint,
//                    key.first
//                    ,key.second
//                    ,key.third))
//
//        } else if (map.get(key) in 1*margin..2*margin) {
//            objects.add(ModelInstance(foursixPoint,
//                    key.first
//                    ,key.second,
//                    key.third))
//
//        } else if (map.get(key) in 3*margin..4*margin) {
//            objects.add(ModelInstance(sevenninePoint,
//                    key.first
//                    ,key.second
//                    ,key.third))
//        } else if (map.get(key) in 4*margin..5*margin) {
//            objects.add(ModelInstance(tentwelvePoint,
//                    key.first
//                    , key.second
//                    , key.third))
//        } else if (map.get(key) in 5*margin..6*margin) {
//            objects.add(ModelInstance(thriteenfifteenPoint,
//                    key.first
//                    , key.second
//                    , key.third))
//        } else if (map.get(key) in 6*margin..7*margin) {
//            objects.add(ModelInstance(morePoint,
//                    key.first
//                    , key.second
//                    , key.third))
//        }
//    }
//
//    return  objects
//}


//        modelBatch!!.begin(cam)
//        val standIn = shave()
//        val standIn = getNewCoord()
//        for (inst in standIn) {
//            if(isVisible(inst)) {
//                modelBatch!!.render(inst, environment)
//                renderedCount++
//            }
//        }
//        modelBatch!!.end()

//
//fun isVisible(inst:ModelInstance):Boolean{
//    var position = Vector3()
//    inst!!.transform.getTranslation(position);
//    return cam!!.frustum.pointInFrustum(position);
//}
