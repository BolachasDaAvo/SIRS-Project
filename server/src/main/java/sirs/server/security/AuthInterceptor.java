package sirs.server.security;

import io.grpc.*;

public class AuthInterceptor implements ServerInterceptor {
    public static final Context.Key<Integer> USER_ID = Context.key("userId");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        int userId = -1;
        try {
            String token = metadata.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)).substring(7);
            userId = JwtTokenProvider.extractId(token);
        } catch (Exception e) {
        }
        Context context = Context.current().withValue(USER_ID, userId);
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
    }
}