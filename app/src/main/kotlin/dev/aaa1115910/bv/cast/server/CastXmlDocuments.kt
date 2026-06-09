package dev.aaa1115910.bv.cast.server

object CastXmlDocuments {
    const val SINK_PROTOCOL_INFO =
        "http-get:*:*:*," +
            "http-get:*:video/mp4:*," +
            "http-get:*:video/x-matroska:*," +
            "http-get:*:video/x-msvideo:*," +
            "http-get:*:video/x-flv:*," +
            "http-get:*:video/octet-stream:*," +
            "http-get:*:video/mpeg:*," +
            "http-get:*:video/quicktime:*," +
            "http-get:*:application/vnd.apple.mpegurl:*," +
            "http-get:*:application/octet-stream:*," +
            "http-get:*:application/x-mpegURL:*," +
            "http-get:*:application/dash+xml:*," +
            "http-get:*:audio/mpeg:*," +
            "http-get:*:audio/mp4:*," +
            "http-get:*:audio/flac:*," +
            "http-get:*:audio/x-flac:*," +
            "http-get:*:audio/wav:*," +
            "http-get:*:image/jpeg:*," +
            "http-get:*:image/png:*"

    fun deviceDescription(host: String, uuid: String): String {
        val baseUrl = "http://$host:${CastReceiverConfig.HTTP_PORT}/bilibili"
        return xml(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <specVersion>
                <major>1</major>
                <minor>0</minor>
              </specVersion>
              <URLBase>$baseUrl/</URLBase>
              <device>
                <deviceType>${CastReceiverConfig.MEDIA_RENDERER_DEVICE_TYPE}</deviceType>
                <friendlyName>${CastReceiverConfig.DEVICE_NAME}</friendlyName>
                <manufacturer>${CastReceiverConfig.MANUFACTURER}</manufacturer>
                <manufacturerURL>https://www.bilibili.com/</manufacturerURL>
                <modelDescription>NeoBV Bilibili projection receiver</modelDescription>
                <modelName>${CastReceiverConfig.MODEL_NAME}</modelName>
                <modelNumber>${CastReceiverConfig.MODEL_NUMBER}</modelNumber>
                <UDN>uuid:$uuid</UDN>
                <serviceList>
                  <service>
                    <serviceType>${CastReceiverConfig.AV_TRANSPORT_SERVICE_TYPE}</serviceType>
                    <serviceId>urn:upnp-org:serviceId:AVTransport</serviceId>
                    <SCPDURL>/bilibili/AVTransport.xml</SCPDURL>
                    <controlURL>/bilibili/AVTransport/control</controlURL>
                    <eventSubURL>/bilibili/AVTransport/event</eventSubURL>
                  </service>
                  <service>
                    <serviceType>${CastReceiverConfig.RENDERING_CONTROL_SERVICE_TYPE}</serviceType>
                    <serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId>
                    <SCPDURL>/bilibili/RenderingControl.xml</SCPDURL>
                    <controlURL>/bilibili/RenderingControl/control</controlURL>
                    <eventSubURL>/bilibili/RenderingControl/event</eventSubURL>
                  </service>
                  <service>
                    <serviceType>${CastReceiverConfig.CONNECTION_MANAGER_SERVICE_TYPE}</serviceType>
                    <serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId>
                    <SCPDURL>/bilibili/ConnectionManager.xml</SCPDURL>
                    <controlURL>/bilibili/ConnectionManager/control</controlURL>
                    <eventSubURL>/bilibili/ConnectionManager/event</eventSubURL>
                  </service>
                  <service>
                    <serviceType>${CastReceiverConfig.NIRVANA_SERVICE_TYPE}</serviceType>
                    <serviceId>urn:app-bilibili-com:serviceId:NirvanaControl</serviceId>
                    <SCPDURL>/bilibili/NirvanaControl.xml</SCPDURL>
                    <controlURL>/bilibili/NirvanaControl/control</controlURL>
                    <eventSubURL>/bilibili/NirvanaControl/event</eventSubURL>
                  </service>
                </serviceList>
              </device>
            </root>
            """
        )
    }

    fun avTransportScpd(): String = serviceScpd(
        actions = listOf(
            action("SetAVTransportURI", "InstanceID", "CurrentURI", "CurrentURIMetaData"),
            action("Play", "InstanceID", "Speed"),
            action("Pause", "InstanceID"),
            action("Stop", "InstanceID"),
            action("Seek", "InstanceID", "Unit", "Target"),
            actionSpec("GetTransportInfo", listOf(
                inArg("InstanceID"),
                outArg("CurrentTransportState"),
                outArg("CurrentTransportStatus"),
                outArg("CurrentSpeed")
            )),
            actionSpec("GetPositionInfo", listOf(
                inArg("InstanceID"),
                outArg("Track"),
                outArg("TrackDuration"),
                outArg("TrackMetaData"),
                outArg("TrackURI"),
                outArg("RelTime"),
                outArg("AbsTime"),
                outArg("RelCount"),
                outArg("AbsCount")
            )),
            actionSpec("GetMediaInfo", listOf(
                inArg("InstanceID"),
                outArg("NrTracks"),
                outArg("MediaDuration"),
                outArg("CurrentURI"),
                outArg("CurrentURIMetaData"),
                outArg("NextURI"),
                outArg("NextURIMetaData"),
                outArg("PlayMedium"),
                outArg("RecordMedium"),
                outArg("WriteStatus")
            )),
            actionSpec("GetCurrentTransportActions", listOf(
                inArg("InstanceID"),
                outArg("Actions")
            )),
            actionSpec("GetDeviceCapabilities", listOf(
                inArg("InstanceID"),
                outArg("PlayMedia"),
                outArg("RecMedia"),
                outArg("RecQualityModes")
            )),
            actionSpec("GetTransportSettings", listOf(
                inArg("InstanceID"),
                outArg("PlayMode"),
                outArg("RecQualityMode")
            ))
        )
    )

    fun renderingControlScpd(): String = serviceScpd(
        actions = listOf(
            action("GetMute", "InstanceID", "Channel"),
            action("SetMute", "InstanceID", "Channel", "DesiredMute"),
            action("GetVolume", "InstanceID", "Channel"),
            action("SetVolume", "InstanceID", "Channel", "DesiredVolume")
        )
    )

    fun connectionManagerScpd(): String = serviceScpd(
        actions = listOf(
            action("GetProtocolInfo"),
            actionSpec("PrepareForConnection", listOf(
                inArg("RemoteProtocolInfo"),
                inArg("PeerConnectionManager"),
                inArg("PeerConnectionID"),
                inArg("Direction"),
                outArg("ConnectionID"),
                outArg("AVTransportID"),
                outArg("RcsID")
            )),
            action("GetCurrentConnectionIDs"),
            action("GetCurrentConnectionInfo", "ConnectionID")
        )
    )

    fun nirvanaControlScpd(): String = serviceScpd(
        actions = listOf(
            actionSpec("GetAppInfo", listOf(
                outArg("PackageName"),
                outArg("AppKey"),
                outArg("Signature"),
                outArg("CurrentSignedIn")
            )),
            action("LoginWithCode", "Code"),
            actionSpec("PrepareForMirrorProjection", listOf(
                outArg("ScreenWidth"),
                outArg("ScreenHeight"),
                outArg("PushUrl")
            )),
            action("SetDanmakuSwitch", "DesiredSwitch"),
            action("AppendDanmaku", "Content", "Size", "Type", "Color", "DanmakuId", "Action"),
            actionSpec("GetPlayInfo", listOf(
                inArg("Params"),
                outArg("Content")
            )),
            actionSpec("GetAccountInfo", listOf(outArg("VipInfo"))),
            action("SwitchQuality", "Qn"),
            action("Play"),
            action("Pause"),
            action("Stop"),
            action("Seek", "Target"),
            action("SetSpeed", "Speed")
        )
    )

    private fun serviceScpd(actions: List<String>): String = xml(
        """
        <?xml version="1.0" encoding="utf-8"?>
        <scpd xmlns="urn:schemas-upnp-org:service-1-0">
          <specVersion>
            <major>1</major>
            <minor>0</minor>
          </specVersion>
          <actionList>
            ${actions.joinToString(separator = "\n")}
          </actionList>
          <serviceStateTable>
            <stateVariable sendEvents="no">
              <name>A_ARG_TYPE_InstanceID</name>
              <dataType>ui4</dataType>
            </stateVariable>
            <stateVariable sendEvents="no">
              <name>A_ARG_TYPE_String</name>
              <dataType>string</dataType>
            </stateVariable>
          </serviceStateTable>
        </scpd>
        """
    )

    private fun action(name: String, vararg args: String): String {
        return actionSpec(name, args.map { inArg(it) })
    }

    private fun actionSpec(name: String, args: List<ScpdArg>): String {
        val argumentList = args.joinToString(separator = "\n") { arg ->
            """
            <argument>
              <name>${arg.name}</name>
              <direction>${arg.direction}</direction>
              <relatedStateVariable>${arg.relatedStateVariable}</relatedStateVariable>
            </argument>
            """.trimIndent()
        }
        return """
            <action>
              <name>$name</name>
              <argumentList>
                $argumentList
              </argumentList>
            </action>
        """.trimIndent()
    }

    private fun inArg(name: String): ScpdArg = ScpdArg(name = name, direction = "in")

    private fun outArg(name: String): ScpdArg = ScpdArg(name = name, direction = "out")

    private data class ScpdArg(
        val name: String,
        val direction: String,
        val relatedStateVariable: String = "A_ARG_TYPE_String"
    )

    private fun xml(value: String): String =
        value.trimIndent().lineSequence().joinToString("\n") { it.trimEnd() }
}
