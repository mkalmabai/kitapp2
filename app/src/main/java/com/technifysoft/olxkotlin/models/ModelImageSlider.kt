package com.technifysoft.olxkotlin.models

class ModelImageSlider {

    /*---Variables. spellings and case should be same as in firebase db [Ads > AdId > Images > ...]---*/
    var id: String = ""
    var imageUrl: String = ""

    /*---Empty constructor require for firebase db---*/
    constructor()

    /*---Constructor with all params---*/
    constructor(id: String, imageUrl: String) {
        this.id = id
        this.imageUrl = imageUrl
    }


}