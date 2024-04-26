package com.technifysoft.olxkotlin.models

class ModelChat {

    /*---Variables. spellings and case should be same as in firebase db---*/
    var messageId: String = ""
    var messageType: String = ""
    var message: String = ""
    var fromUid: String = ""
    var toUid: String = ""
    var timestamp: Long = 0

    /*---Empty constructor require for firebase db---*/
    constructor()

    /*---Constructor with all params---*/
    constructor(
        messageId: String,
        messageType: String,
        message: String,
        fromUid: String,
        toUid: String,
        timestamp: Long
    ) {
        this.messageId = messageId
        this.messageType = messageType
        this.message = message
        this.fromUid = fromUid
        this.toUid = toUid
        this.timestamp = timestamp
    }


}