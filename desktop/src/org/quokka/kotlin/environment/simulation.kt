package org.quokka.kotlin.environment

import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt


fun main(){
    val aux = Simulation()
//    aux.runSim(500,500,400,100000f, 65.5f,1.55f,1)
//    aux.runMath(400f,1000000000f, 55f,1.55f,1)

//    aux.runMe(5000f, 1000000f, 60f, 1.6f,1)

    aux.aneaw(1000f, 100000f, 60f, 1.6f,1)
}



/*



. Little ZsGreen or bacteria were detected more than 1,500 μm from this boundary.
    Across all tumors, over 80% of bacteria and ZsGreen molecules were found within transition regions.

    The average distance that ZsGreen diffused from the edge of colonies was 6.5 ± 1.0 μm

    Based on a diffusion coefficient, D , for ZsGreen of 1.94 × 10−6 cm2/s

    The number of colonies with extracellular ZsGreen decreased 1.1% every 100 μm into the tumor. Near the periphery (region i ),
    80% had extracellular ZsGreen, which dropped to less than 10% in the necrotic core (region iv ).
    Position within tumors did not affect average colony size

    Viable regions (region i ) had the highest production rate (m=7.6 fg·CFU−1·hr−1),
    compared to necrotic regions, (m= 0.47 fg·CFU−1·hr−1; region iv ; p  < 0.05).

    Tumor‐detecting bacteria colonized tumors at a density of 485.3 CFU·mg−
    and released ZsGreen at a rate of 4.3 fg·CFU−1·hr−1
    
 */

class Simulation() {

    class SpaceUnit( var reg: Int, bacc: Int){
        var colony = bacc              // nr o bacteria

        var florecentLVL = 0f              // amout of flocecent currently in

        var prod_rate = 4.3F                 //release rate of bacter colony, in mol/time

        var transition = false

        fun updateprod_rate(newReg : Float){
            prod_rate = newReg
        }

        fun setTransitionReg( bol : Boolean){
            this.transition = bol
        }

        fun updateColony(newcol : Int){
            this.colony = newcol
        }

        fun updateFlorecent(newflorecent : Float){
            this.florecentLVL = florecentLVL + newflorecent
        }

    }

    class SimulationInstance(board: Array<Array<SpaceUnit>>){

        val myboard = board

        var plasmaFlo = 0f

        fun updatePlamsaFlo(newf: Float ){
            this.plasmaFlo = newf
        }
    }

    companion object {
        const val TRANISTION_PROD = 7.6f        // fg/(CFU*hours) -> Femtogram to micro fg / 1 000 000 000 = microg
        const val NECROTIC_PROD = 0.47f     // fg/(CFU*hours)

        const val Ke = 0.259f   // hours^-1
        const val KV = 0.0125f     // hours^-1

        const val prod_time = 56

        const val COLONY_SIZE = 10

        const val VIABLE_REG = 1
        const val NECRO_REG = 2

        const val MIN_DETECTABLE = 104.2f    //micro gram per liter

        const val X_size = 30      //100 micrometer -> total 1cm^2
        const val Y_size = 30
        const val Z_size = 30

        const val tranBound = 1

        const val bill = 1000000000f
        const val vill = 1000000f
    }

    /*
    parameters

    cancer size and location

    bacteria density

    time

    ping rate
     */

    fun build(x: Int, y: Int, cancerDiameter : Int,CFU: Float): Array<Array<SpaceUnit>>{

//        space =  ArrayList<ArrayList<SpaceUnit>>()

        var florecent_plasma = 0f   // per ml

        var crtBorad = createBoard()

        addCancer(x,y,cancerDiameter,crtBorad)

        addBacteria(crtBorad,CFU)


        return crtBorad

    }

    fun calcVol(diameter: Int){

        val surface = diameter*10f*2f*2f //assumes 1 units depth. diameter its doubled, and doubled again for squre, one unit is 5 micro

        val cm = surface / 1e12

        println("Cancer: surface in cm  $cm and mass $cm")
    }


    fun createBoard():  Array<Array<SpaceUnit>>{

        var space = arrayOf<Array<SpaceUnit>>()
        //set up space
        for (i in 0..X_size) {
            var auxarray = arrayOf<SpaceUnit>()
            for (j in 0..Y_size) {
                auxarray += SpaceUnit(VIABLE_REG, 0)
            }
            space += auxarray
        }

        return space
    }

    fun createBoard3d():  Array<Array<Array<SpaceUnit>>>{

        var space = arrayOf<Array<Array<SpaceUnit>>>()
        //set up space
        for (i in 0..X_size) {
            var auxarray = arrayOf<Array<SpaceUnit>>()
            for (j in 0..Y_size) {
                var rayray = arrayOf<SpaceUnit>()
                for (k in 0..Z_size){
                    rayray += SpaceUnit(VIABLE_REG, 0)
                }
                auxarray += rayray
            }
            space += auxarray
        }

        return space
    }

    fun addCancer(x: Int, y: Int, cancerDiameter : Int, space : Array<Array<SpaceUnit>>){

        val topleftx = x - cancerDiameter
        val toplefty = y - cancerDiameter
        val bottomrightx = x + cancerDiameter
        val bottomrighty = y + cancerDiameter

        //add nectroic tisues
        for (i in topleftx..bottomrightx) {
            for (j in toplefty..bottomrighty) {
                if (i >= 0 && j >= 0 && i < X_size && j < Y_size) {
//                    space[i][j].setp(NECRO_REG)
                }
            }
        }

        // add trasitiona regions
        for (i in topleftx - tranBound..bottomrightx + tranBound){
            for (j in toplefty - tranBound..bottomrighty + tranBound) {
                if (i >= 0 && j >= 0 && i < X_size && j < Y_size) {
                    space[i][j].setTransitionReg(true)
                }
            }
         }


        //clearnout not trasition region in cancer
        if (topleftx + tranBound <= bottomrighty - tranBound) {
            for (i in topleftx + tranBound..bottomrightx - tranBound) {
                for (j in toplefty + 5..bottomrighty - 5) {
                    if (i >= 0 && j >= 0 && i < X_size && j < Y_size) {
                        space[i][j].setTransitionReg(false)
                    }
                }
            }
        }

    }



    fun addBacteria(space : Array<Array<SpaceUnit>>, CFU: Float) {

        var nrCol = CFU / COLONY_SIZE * 53 / 100   //10 bacteria per colony, only 53% prod floouracent

        var transCol = (nrCol * 8f / 10f).toInt()  /2    //80% of bacteria in trans

        var necroCol = (nrCol * 1.5f / 10f).toInt()

        //INCORRECT  bacteria can be in transition region outside of the cancer
        //TODO
        for (i in 0..X_size step 2) {
            for (j in 0..Y_size step 2) {
                if (space[i][j].reg == NECRO_REG) {
                    if (space[i][j].transition == true && transCol > 0) {
                        space[i][j].colony = 2
                        transCol-=2
                    } else if (space[i][j].transition == false && necroCol > 0) {
                        space[i][j].colony = 2
                        necroCol-=2
                    }
                }
            }
        }
        println("leftover trans $transCol")
        println("leftover necro $necroCol" )
    }

    /*
        bacteria found to produce flocerencet for 58 hours

        each tick florecent is released b bacteria into the spaceUnit
        each tick each spcae unit within the transition and necro releases into blood
        each tick florecent is leanred from the blood at clearance rate

        53% of bacateria in tumour produce

        florecent is 104.2 g/mol ->  104.2 microg/micromol
        min detectable : 1 micromol/l
        min detectable -> 104.2microg f/ l

        1.0 g*ml−1 tumor weiht conversion
     */



    fun getPlamsmaVol(weight: Float, height: Float, sex: Int): Float{

        val BMI = weight/height.pow(2)
        val BV = 70f/ sqrt(BMI/22)
        val blood_volume : Float


        if(sex == 1){   //fem
            blood_volume = weight*14148f/(8780f+244f * BMI) * BV
        } else if (sex == 2) { //male
            blood_volume = weight * 11432f/(6680f +216f * BMI) *BV
        } else {
            error("two genders da dum tzzzz")
        }
        println("blood vol : ${blood_volume.div(1000f)}")
        return (blood_volume * 5.8f / 10f).div(1000f)

    }

    /**
     * @param CFU nr of bacteria
     *0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * v
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     * 0.124 g with a bacterial density 485.3 CFU?mg−1
     *
     * IDEA ONE SUQRE IS ONE MILIGRAM
     *
     * 1 g = 1000 mg
     *
     *
     */

    fun printBoard(space : Array<Array<SpaceUnit>>){
        for (i in 0..X_size) {
            val string = StringBuilder()
            for (j in 0..Y_size) {
                if(space[i][j].transition == false) {
                    string.append(space[i][j].reg).append(" ")
                } else {
                    string.append("1").append(" ")
                }
            }
            println(string.toString())
        }
    }

    /*
   minimum detection is 1 micro mol per l

   Vt *Ct/t = m Cb Vt - kv Vt (Ct-Cp)
   Vp Cp/t = kv Vt (Ct- Cp) - Ke Cp Vp

   Vp  - plasma volume ml
   Vt  - tumor volume ml
   kv  - mass transfer rate contant (1/h) = 0.0125
   m   - rate of Zgreen prod per bacetrium (fg /CFU h)
   Cp  - ZsGreen concentration  in plasma (ng·ml‐1)
   Ct  - ZsGreen concentration  in tumour (ng·ml‐1)
   Cb  - tumor bacterial density (CFU/ml)
   Ke  - plasma clearance rate (1/h) = 0.259

    */


    fun aneaw(cancer_vol : Float, CFU: Float, weight: Float,height: Float,sex: Int){

        var space = createBoard3d()

        val plasma = getPlamsmaVol(weight,height,sex)

        val min_detectable = plasma * MIN_DETECTABLE
        println("min detectable $min_detectable")

        val prod_rate = (486f*4.3f).div(1e9f)  //9 + 3

        var flo_in_plasma = 0f
        var flo_in_cancer = 0f

        var  detection_time = 0f
        var empty_time = 0f
        var flag_detecte = 0
        var flag_empty = 0

        var prev_flag = 0

        var Ct = 0f
        var Cp = 0f
        var transfer = 0f

        var prodtime = prod_time
        for (tik in 0..1000) {
            prodtime--
            transfer = KV * (Ct - Cp) * cancer_vol

            if (prodtime > 0) {
                flo_in_cancer += prod_rate*cancer_vol
            }

//            flo_in_cancer += prod_rate*cancer_vol



            flo_in_cancer  -= transfer

            Ct = flo_in_cancer / cancer_vol

            flo_in_plasma += transfer

            if (flo_in_plasma < Ke * Cp * plasma){
                flo_in_plasma = 0f
            } else {
                flo_in_plasma -= Ke * Cp * plasma
            }

            Cp = flo_in_plasma / plasma

 //       Vt *Ct/t = m Cb Vt - kv Vt (Ct-Cp)
//        Vp Cp/t = kv Vt (Ct- Cp) - Ke Cp Vp





            if (flo_in_plasma >= min_detectable) {
                detection_time = tik.toFloat()
                flag_detecte = 1
            } else {
                flag_detecte = 0
            }

            if(flag_detecte == 1 && flag_detecte == prev_flag ){
                empty_time++
            }

            prev_flag = flag_detecte

        }


        var days = detection_time/24
        var hours = detection_time % 24
        println("Detectable at $days days , $hours h")
        days = empty_time/24
        hours = empty_time % 24
        println("Florecent stayed up for $empty_time h or $days days , $hours h ")
        println("Final flo_plamsa : ${Cp * plasma}")
        println("Final flo_plamsa : ${flo_in_plasma}")


    }

    //            println("flo in plamsa $flo_in_plasma at time $tik")

//            for (i in 0..X_size) {
//                for (j in 0..Y_size) {
//                    for (k in 0..Z_size) {
//                        space[i][j][k].updateFlorecent(prod_rate)
//                        val ughhg = space[i][j][k].florecentLVL
//
//                        flo_concentr += (ughhg - flo_concentr)* KV
//                        space[i][j][k].updateFlorecent(-1*(ughhg - flo_concentr)* KV)
//
//                    }
//                }
//            }

//    Ct += prod_rate
//    transfer = KV*(Ct-Cp)
//    if(Ct < transfer){
////                println("${Ct} vs ${transfer} at $tik")
//    } else {
//        Ct -= transfer
//    }
//    flo_in_plasma += transfer*cancer_vol
//    flo_in_plasma -= Ke * flo_in_plasma
//
//    Cp = flo_in_plasma / plasma / cancer_vol
//            println("whatver this is: ${transfer*cancer_vol/plasma}")
//            println("CP before $Cp")
//            println("Cp after $Cp")
//            if (KV*(Ct-Cp)*cancer_vol/plasma < Ke*Cp){
//                println("${KV*(Ct-Cp)*cancer_vol/plasma} vs ${Ke*Cp}")
//            }
}

//fun runSim(x: Int, y: Int, cancer_size: Int, CFU: Float, weight: Float, height: Float, sex : Int){
//
//    val simm = Simulation.SimulationInstance(build(x, y, cancer_size, CFU))
//
//    calcVol(cancer_size)
//    calcVol(500)
//
//    val plasma_volume = getPlamsmaVol(weight, height, sex)
//    val mindetect = Simulation.MIN_DETECTABLE * plasma_volume
//    println("blood volume $plasma_volume")
//    println("min detectable $mindetect")
//
//    var plasma_florecent = 0f
//
//    var prev = 0f
//    var flag = 0
//
//    val E = 2.71828f
//
//    for (timeTick in 0 .. 1000) {
//
//        for (i in 0..Simulation.X_size) {
//            for (j in 0..Simulation.Y_size) {
//                if (simm.myboard[i][j].reg == Simulation.NECRO_REG) {
//                    //create florecent
//                    if( simm.myboard[i][j].transition == true) {
//                        val aux = Simulation.TRANISTION_PROD * Simulation.COLONY_SIZE / 4 * simm.myboard[i][j].colony
//                        simm.myboard[i][j].updateFlorecent(aux*2)
//
//                        // move florescent
//                        if(i+1 <= Simulation.X_size) {
//                            simm.myboard[i + 1][j].updateFlorecent(aux)
//                        }
//                        if(j+1 <= Simulation.Y_size) {
//                            simm.myboard[i][j + 1].updateFlorecent(aux)
//                        }
//                    } else {
//                        val aux = Simulation.NECROTIC_PROD * Simulation.COLONY_SIZE / 3 * simm.myboard[i][j].colony
//                        simm.myboard[i][j].updateFlorecent(aux*2)
//
//                        // move florescent
//                        if(i+1 <= Simulation.X_size) {
//                            simm.myboard[i + 1][j].updateFlorecent(aux)
//                        }
//                    }
//
//                    //disipate florescent
//                    val floreAway = simm.myboard[i][j].florecentLVL * Simulation.KV
////                        println(floreAway)
//
//                    simm.myboard[i][j].updateFlorecent(-1*floreAway)
//                    plasma_florecent += floreAway
//
//                }
//            }
//        }
//
//        plasma_florecent = plasma_florecent * E.pow(Simulation.Ke *-1)
//
//
////            println(plasma_florecent)
//
//        val aux_plasma = plasma_florecent.div(1000000)       //9 s for micro gram and 6 for nano
//
////            println(aux_plasma)
//        if(aux_plasma >= Simulation.MIN_DETECTABLE * plasma_volume){
//            if(flag == 0){
//                flag = 1
//                val days = timeTick/24
//                val hours = timeTick % 24
//
//                println("Detectable at $days days , $hours h")
//            }
//        } else if (flag == 1){
//            println("no longer detectable at $timeTick")
//        }
//
//        if (prev == aux_plasma){
//            break
//        } else {
//            prev = aux_plasma
//        }
//    }
//    println("done flr lvl $plasma_florecent")
//
//}
