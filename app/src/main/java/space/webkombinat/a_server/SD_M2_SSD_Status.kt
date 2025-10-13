package space.webkombinat.a_server

import androidx.compose.runtime.mutableStateOf

class SD_M2_SSD_Status {

//    var sd_m2_ssd_status = mutableStateOf(false)
//    val sd_m2_ssd_text = "内部SDもしくはUSB(m.2,ssd)があるか"
//
//    val sd_m2_ssd_path_state = mutableStateOf(false)
//    val sd_m2_ssd_path_text = "内部SDもしくはUSB(m.2,ssd)のパスを選んでいるか"

    var strage_ANAS_state = mutableStateOf(false)
    val strage_ANAS_text = "./ANASフォルダを選んでいるか"

    var ANAS_read_state = mutableStateOf(false)
    val ANAS_read_text = "./ANASフォルダ下を読み取ることができるか"

    var ANAS_wirte_state = mutableStateOf(false)
    val ANAS_write_text = "./ANASフォルダ下に書き込むことができるか"
}

//複数のSSDに対応させる