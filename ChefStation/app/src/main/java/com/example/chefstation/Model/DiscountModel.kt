package com.example.chefstation.Model

class DiscountModel {

    var key:String? = ""
    var percent:Int = 0
    var untilDate:Long = 0

    constructor(){}
    constructor(key:String?,percent:Int,untilDate:Long){

        this.key = key
        this.percent = percent
        this.untilDate = untilDate
    }
}