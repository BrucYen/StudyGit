package com.studyguard.app.data

data class Province(val name: String, val cities: List<String>)

object RegionData {
    val provinces: List<Province> = listOf(
        Province("北京市", listOf("北京市")),
        Province("天津市", listOf("天津市")),
        Province("上海市", listOf("上海市")),
        Province("重庆市", listOf("重庆市")),
        Province("河北省", listOf("石家庄市", "唐山市", "保定市", "邯郸市")),
        Province("山西省", listOf("太原市", "大同市", "临汾市", "运城市")),
        Province("内蒙古自治区", listOf("呼和浩特市", "包头市", "赤峰市")),
        Province("辽宁省", listOf("沈阳市", "大连市", "鞍山市", "锦州市")),
        Province("吉林省", listOf("长春市", "吉林市", "四平市")),
        Province("黑龙江省", listOf("哈尔滨市", "齐齐哈尔市", "大庆市")),
        Province("江苏省", listOf("南京市", "苏州市", "无锡市", "南通市", "徐州市")),
        Province("浙江省", listOf("杭州市", "宁波市", "温州市", "金华市", "绍兴市")),
        Province("安徽省", listOf("合肥市", "芜湖市", "阜阳市", "安庆市")),
        Province("福建省", listOf("福州市", "厦门市", "泉州市", "漳州市")),
        Province("江西省", listOf("南昌市", "赣州市", "九江市", "上饶市")),
        Province("山东省", listOf("济南市", "青岛市", "烟台市", "潍坊市", "临沂市")),
        Province("河南省", listOf("郑州市", "洛阳市", "南阳市", "新乡市")),
        Province("湖北省", listOf("武汉市", "宜昌市", "襄阳市", "黄冈市")),
        Province("湖南省", listOf("长沙市", "株洲市", "衡阳市", "常德市")),
        Province("广东省", listOf("广州市", "深圳市", "东莞市", "佛山市", "珠海市")),
        Province("广西壮族自治区", listOf("南宁市", "桂林市", "柳州市", "北海市")),
        Province("海南省", listOf("海口市", "三亚市", "儋州市")),
        Province("四川省", listOf("成都市", "绵阳市", "南充市", "宜宾市")),
        Province("贵州省", listOf("贵阳市", "遵义市", "毕节市")),
        Province("云南省", listOf("昆明市", "曲靖市", "大理市", "丽江市")),
        Province("西藏自治区", listOf("拉萨市", "日喀则市", "林芝市")),
        Province("陕西省", listOf("西安市", "咸阳市", "宝鸡市", "汉中市")),
        Province("甘肃省", listOf("兰州市", "天水市", "酒泉市")),
        Province("青海省", listOf("西宁市", "海东市")),
        Province("宁夏回族自治区", listOf("银川市", "吴忠市", "中卫市")),
        Province("新疆维吾尔自治区", listOf("乌鲁木齐市", "喀什市", "伊宁市")),
        Province("香港特别行政区", listOf("香港")),
        Province("澳门特别行政区", listOf("澳门")),
        Province("台湾省", listOf("台北市", "高雄市", "台中市"))
    )

    fun citiesOf(province: String): List<String> =
        provinces.firstOrNull { it.name == province }?.cities ?: listOf(province)

    fun defaultCity(province: String): String = citiesOf(province).firstOrNull() ?: ""
}
