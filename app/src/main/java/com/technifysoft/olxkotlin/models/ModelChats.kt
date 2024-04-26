package com.technifysoft.olxkotlin.models

class ModelChats {

    /*---Variables. spellings and case should be same as in firebase db---*/
    var profileImageUrl = ""
    var name = ""
    var chatKey = ""
    var receiptUid = ""
    var messageId = ""
    var messageType = ""
    var message = ""
    var fromUid = ""
    var toUid = ""
    var timestamp: Long = 0

    /*---Empty constructor require for firebase db---*/
    constructor()

    /*---Constructor with all params---*/
    constructor(
        profileImageUrl: String,
        name: String,
        chatKey: String,
        receiptUid: String,
        messageId: String,
        messageType: String,
        message: String,
        fromUid: String,
        toUid: String,
        timestamp: Long
    ) {
        this.profileImageUrl = profileImageUrl
        this.name = name
        this.chatKey = chatKey
        this.receiptUid = receiptUid
        this.messageId = messageId
        this.messageType = messageType
        this.message = message
        this.fromUid = fromUid
        this.toUid = toUid
        this.timestamp = timestamp
    }


}