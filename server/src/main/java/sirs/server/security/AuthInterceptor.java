package sirs.server.security;

import io.grpc.*;

public class AuthInterceptor implements ServerInterceptor {
    public static final Context.Key<Integer> USER_ID = Context.key("userId");
    public static final Context.Key<String> USER_TOKEN = Context.key("userToken");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        int userId = -1;
        String token = null;
        try {
            token = metadata.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)).substring(7);
            userId = JwtTokenProvider.extractId(token);
        } catch (Exception e) {
        }
        Context context = Context.current().withValue(USER_ID, userId).withValue(USER_TOKEN, token);
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
    }
}