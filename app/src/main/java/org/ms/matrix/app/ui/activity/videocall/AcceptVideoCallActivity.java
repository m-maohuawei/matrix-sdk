package org.ms.matrix.app.ui.activity.videocall;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.ms.matrix.app.R;
import org.ms.matrix.app.db.MatrixDbInjection;
import org.ms.matrix.app.db.event.Event;
import org.ms.matrix.sdk.client.MatrixClient;
import org.ms.matrix.sdk.model.event.m_call_answer;
import org.ms.matrix.sdk.model.event.m_call_candidates;
import org.ms.matrix.sdk.model.event.m_call_invite;
import org.ms.matrix.sdk.supper.inter.callback.MatrixCallBack;
import org.ms.matrix.sdk.supper.inter.room.IRoom;
import org.ms.module.base.view.BaseAppCompatActivity;
import org.ms.module.supper.client.Modules;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AcceptVideoCallActivity extends BaseAppCompatActivity {


    private static final String TAG = "AcceptVideoCallActivity";

    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;


    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase eglBase;
    private SdpObserver sdpObserver;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private List<String> streamList;
    private DataChannel channel;


    private VideoTrack videoTrack;
    private AudioTrack audioTrack;


    private static int count;
    private List<m_call_candidates.Candidate> candidates = new ArrayList<>();

    private String call_id;

    @Override
    protected int getLayout() {
        return R.layout.activity_accept_video_call;
    }

    @Override
    protected void initView() {
        super.initView();

        localSurfaceView = findViewById(R.id.localSurfaceView);
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView);
    }

    private IRoom room;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPeerConnection();
        String roomid = getIntent().getStringExtra("roomid");


        MatrixClient.getInstance().getRooms().getRoom(roomid, new MatrixCallBack<IRoom, Throwable>() {
            @Override
            public void onSuccess(IRoom iRoom) {
                room = iRoom;
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });


        String content = getIntent().getStringExtra("content");
        if (content != null)
            Modules.getUtilsModule().getThreadPoolUtils().runSubThread(new Runnable() {
                @Override
                public void run() {
                    try {

                        JSONObject jsonObject = new JSONObject(content);
                        call_id = jsonObject.getString("call_id");

                        Log.e(TAG, "run:获取的call_id" + call_id);

                        Log.e(TAG, "run: " + content);
                        m_call_invite.Invtie invtie = Modules.getUtilsModule().getGsonUtils().fromJson(content, m_call_invite.Invtie.class);
                        m_call_invite.Invtie.Offer offer = invtie.getOffer();
                        SessionDescription sessionDescription = null;

                        if (offer.getType().equals(SessionDescription.Type.OFFER.name())) {
                            sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, offer.getSdp());
                        } else if (offer.getType().equals(SessionDescription.Type.ANSWER.name())) {
                            sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, offer.getSdp());
                        } else if (offer.getType().equals(SessionDescription.Type.PRANSWER.name())) {
                            sessionDescription = new SessionDescription(SessionDescription.Type.PRANSWER, offer.getSdp());
                        }


                        Log.e(TAG, "run: 设置 setRemoteDescription");

                        peerConnection.setRemoteDescription(sdpObserver, sessionDescription);

                        MediaConstraints mediaConstraints = new MediaConstraints();
                        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
                        peerConnection.createAnswer(sdpObserver, mediaConstraints);


                        List<Event> events = MatrixDbInjection.providerEventDataSource().queryEventByRoomIdAndType(roomid, "m.call.candidates");

                        for (Event event : events) {
                            String content1 = event.get_content();

                            JSONObject jsonObject1 = new JSONObject(content1);

                            String call_id1 = jsonObject1.getString("call_id");

                            if (call_id.equals(call_id1)) {


                                m_call_candidates.Candidates candidates = Modules.getUtilsModule().getGsonUtils().fromJson(content1, m_call_candidates.Candidates.class);


                                List<m_call_candidates.Candidate> candidates1 = candidates.getCandidates();

                                for (m_call_candidates.Candidate it : candidates1) {


                                    IceCandidate iceCandidate = new IceCandidate(it.getSdpMid(), it.getSdpMLineIndex(), it.getCandidate());

                                    peerConnection.addIceCandidate(iceCandidate);

                                }
                            }
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
    }


    /**
     * 连接
     */
    public void createPeerConnection() {

        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        //创建EglBase对象
        eglBase = EglBase.create();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = true;
        options.disableNetworkMonitor = true;
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();
        // 配置STUN穿透服务器  转发服务器
        iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();

        PeerConnection.IceServer var11 = PeerConnection.IceServer.builder("stun:39.106.112.201:3478?transport=udp")
                .createIceServer();
        PeerConnection.IceServer var12 = PeerConnection.IceServer.builder("turn:39.106.112.201:3478?transport=udp")
                .setUsername("bdlbsc")
                .setPassword("123456")
                .createIceServer();
        PeerConnection.IceServer var13 = PeerConnection.IceServer.builder("turn:39.106.112.201:3478?transport=tcp")
                .setUsername("bdlbsc")
                .setPassword("123456")
                .createIceServer();


        iceServers.add(iceServer);
        iceServers.add(var11);
        iceServers.add(var12);
        iceServers.add(var13);

        streamList = new ArrayList<>();

        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);

        PeerConnectionObserver connectionObserver = new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);


                Log.e(TAG, "onIceCandidate: 第" + count++ + "此回调");

                Log.e(TAG, "onIceCandidate sdp :" + iceCandidate.sdp);
                Log.e(TAG, "onIceCandidate sdpMid : " + iceCandidate.sdpMid);
                Log.e(TAG, "onIceCandidate serverUrl : " + iceCandidate.serverUrl);
                Log.e(TAG, "onIceCandidate sdpMlineIndex : " + iceCandidate.sdpMLineIndex);
                Log.e(TAG, "onIceCandidate toString : " + iceCandidate.toString());

                // ice 信息


                candidates.add(m_call_candidates.Candidate.builder().sdpMLineIndex(iceCandidate.sdpMLineIndex)
                        .sdpMid(iceCandidate.sdpMid)
                        .candidate(iceCandidate.sdp)
                        .build()
                );
                m_call_candidates.Candidates build = m_call_candidates.Candidates.builder().call_id(call_id)
                        .candidates(candidates)
                        .version(0).build();


                Log.e(TAG, "onIceGatheringChange: =============================");
                Modules.getLogModule().json(TAG, build.toJson());
                Log.e(TAG, "onIceGatheringChange: =============================");

                if (room != null)
                    room.sendCallCandidates(build, new MatrixCallBack() {
                        @Override
                        public void onSuccess(Object o) {

                        }

                        @Override
                        public void onFailure(Object o) {

                        }
                    });
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);


            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                Log.d(TAG, "onAddStream : " + mediaStream.toString());
                List<VideoTrack> videoTracks = mediaStream.videoTracks;
                if (videoTracks != null && videoTracks.size() > 0) {
                    VideoTrack videoTrack = videoTracks.get(0);
                    if (videoTrack != null) {
                        videoTrack.addSink(remoteSurfaceView);
                    }
                }
                List<AudioTrack> audioTracks = mediaStream.audioTracks;
                if (audioTracks != null && audioTracks.size() > 0) {
                    AudioTrack audioTrack = audioTracks.get(0);
                    if (audioTrack != null) {
                        audioTrack.setVolume(10);
                    }
                }
            }
        };
        peerConnection = peerConnectionFactory.createPeerConnection(configuration, connectionObserver);


        /*
        DataChannel.Init 可配参数说明：
        ordered：是否保证顺序传输；
        maxRetransmitTimeMs：重传允许的最长时间；
        maxRetransmits：重传允许的最大次数；
        */
        DataChannel.Init init = new DataChannel.Init();
        if (peerConnection != null) {
            channel = peerConnection.createDataChannel("channel", init);
        }
        DataChannelObserver channelObserver = new DataChannelObserver();
        connectionObserver.setObserver(channelObserver);


        initSurfaceview(localSurfaceView);
        initSurfaceview(remoteSurfaceView);
        startLocalVideoCapture(localSurfaceView);
        startLocalAudioCapture();


        sdpObserver = new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {


                Log.e(TAG, "onCreateSuccess: " + sessionDescription.description);
                Log.e(TAG, "onCreateSuccess: " + sessionDescription.type.name());

                //将会话描述设置在本地
                peerConnection.setLocalDescription(this, sessionDescription);
                SessionDescription localDescription = peerConnection.getLocalDescription();
                SessionDescription.Type type = localDescription.type;

                //接下来使用之前的WebSocket实例将offer发送给服务器
                if (type == SessionDescription.Type.OFFER) {
                    //呼叫 2

                } else if (type == SessionDescription.Type.ANSWER) {
                    //应答

                    if (room != null) {
                        Log.e(TAG, "onCreateSuccess: 发送应答");
                        m_call_answer.Content build = m_call_answer.Content.builder().call_id(call_id)
                                .lifetime(6000)
                                .version(0)
                                .answer(m_call_answer.Answer.builder()
                                        .sdp(sessionDescription.description)
                                        .type(sessionDescription.type.name())
                                        .build())
                                .build();
                        room.sendCallAnswer(build, new MatrixCallBack() {
                            @Override
                            public void onSuccess(Object o) {

                            }

                            @Override
                            public void onFailure(Object o) {

                            }
                        });
                    } else {
                        Log.e(TAG, "onCreateSuccess: room 为空");
                    }

                } else if (type == SessionDescription.Type.PRANSWER) {
                    //再次应答

                }
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        };
    }


    /**
     * 创建本地音频
     */
    private void startLocalAudioCapture() {
        //语音
        MediaConstraints audioConstraints = new MediaConstraints();
        //回声消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        //自动增益
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        //高音过滤
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        //噪音处理
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        audioTrack = peerConnectionFactory.createAudioTrack("audiotrack", audioSource);
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream("localVideoStream");
        localMediaStream.addTrack(audioTrack);
        // 设置音量
        audioTrack.setVolume(10);
        peerConnection.addTrack(audioTrack, streamList);
        peerConnection.addStream(localMediaStream);
    }

    /**
     * 初始化iew
     *
     * @param localSurfaceView
     */
    private void initSurfaceview(SurfaceViewRenderer localSurfaceView) {
        localSurfaceView.init(eglBase.getEglBaseContext(), null);
        localSurfaceView.setMirror(true);
        localSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        localSurfaceView.setKeepScreenOn(true);
        localSurfaceView.setZOrderMediaOverlay(true);
        localSurfaceView.setEnableHardwareScaler(false);
    }

    /**
     * 创建本地视频
     *
     * @param localSurfaceView
     */
    private void startLocalVideoCapture(SurfaceViewRenderer localSurfaceView) {
        VideoSource videoSource = peerConnectionFactory.createVideoSource(true);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext());
        VideoCapturer videoCapturer = createVideoCapturer();
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(320, 240, 60); // width, height, frame per second
        videoTrack = peerConnectionFactory.createVideoTrack("videtrack", videoSource);
        videoTrack.addSink(localSurfaceView);
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream("localVideoStream");
        localMediaStream.addTrack(videoTrack);
        peerConnection.addTrack(videoTrack, streamList);
        peerConnection.addStream(localMediaStream);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    /**
     * 准备摄像头
     *
     * @return
     */
    private VideoCapturer createVideoCapturer() {
        if (Camera2Enumerator.isSupported(this)) {
            return createCameraCapturer(new Camera2Enumerator(this));
        } else {
            return createCameraCapturer(new Camera1Enumerator(true));
        }
    }


    /**
     * 邀请通话开发 1
     */
    public void offerVoice() {


        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(sdpObserver, mediaConstraints);
    }


    public void addIceCandidate(IceCandidate ice) {
        if (peerConnection != null) {
            peerConnection.addIceCandidate(ice);
        }
    }
}
