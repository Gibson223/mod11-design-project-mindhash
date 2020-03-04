//package org.quokka.kotlin.Enviroment
//
//import LidarData.LidarReader
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
//}