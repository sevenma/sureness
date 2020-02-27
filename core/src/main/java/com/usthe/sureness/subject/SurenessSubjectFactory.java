package com.usthe.sureness.subject;

import com.usthe.sureness.processor.exception.UnsupportedTokenException;
import com.usthe.sureness.subject.Subject;
import com.usthe.sureness.subject.SubjectCreate;
import com.usthe.sureness.subject.SubjectFactory;
import com.usthe.sureness.subject.support.JwtSubject;
import com.usthe.sureness.subject.support.NoneSubject;
import com.usthe.sureness.subject.support.PasswordSubject;
import com.usthe.sureness.util.JsonWebTokenUtil;
import com.usthe.sureness.util.SurenessCommonUtil;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * @author tomsun28
 * @date 23:35 2019-05-12
 */
public class SurenessSubjectFactory implements SubjectFactory {

    private static final String BEARER = "Bearer";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic";
    private static final int COUNT_2 = 2;

    @Override
    public List<Subject> createSubjects(Object request) {
        List<Subject> subjectList = new LinkedList<>();
        if (request instanceof ServletRequest) {
            String authorization = ((HttpServletRequest)request).getHeader(AUTHORIZATION);
            // 根据head里面的参数内容，判断其请求认证鉴权的方式，新建对应的token
            // 现在支持 json web token, Basic auth, ...
            // ("Authorization", "Bearer eyJhbGciOiJIUzUxMi...")  --- jwt auth
            // ("Authorization", "Basic YWRtaW46YWRtaW4=")        --- basic auth
            if (authorization != null && authorization.startsWith(BEARER)) {
                // jwt token
                String jwtValue = authorization.replace(BEARER, "").trim();
                if (JsonWebTokenUtil.isNotJsonWebToken(jwtValue)) {
                    throw new UnsupportedTokenException("Bearer token now support jwt");
                }
                String remoteHost = ((HttpServletRequest) request).getRemoteHost();
                String requestUri = ((HttpServletRequest) request).getRequestURI();
                String requestType = ((HttpServletRequest) request).getMethod();
                String targetUri = requestUri.concat("===").concat(requestType.toUpperCase());
                String userAgent = SurenessCommonUtil.findUserAgent((HttpServletRequest) request);
                subjectList.add(JwtSubject.builder(jwtValue)
                        .setRemoteHost(remoteHost)
                        .setTargetResource(targetUri)
                        .setUserAgent(userAgent)
                        .build());
                // todo 暂时先这样
            } else if (authorization != null && authorization.startsWith(BASIC)) {
                //basic auth
                String basicAuth = authorization.replace(BASIC, "").trim();
                basicAuth = new String(Base64.getDecoder().decode(basicAuth), StandardCharsets.UTF_8);
                String[] auth = basicAuth.split(":");
                if (auth.length != COUNT_2) {
                    throw new UnsupportedTokenException("can not create token due the request message");
                }
                String username = auth[0];
                if (username == null || "".equals(username)) {
                    throw new UnsupportedTokenException("the appId can not null");
                }
                String password = auth[1];
                String remoteHost = ((HttpServletRequest) request).getRemoteHost();
                String requestUri = ((HttpServletRequest) request).getRequestURI();
                String requestType = ((HttpServletRequest) request).getMethod();
                String targetUri = requestUri.concat("===").concat(requestType.toUpperCase());
                subjectList.add(PasswordSubject.builder(username, password)
                        .setRemoteHost(remoteHost)
                        .setTargetResource(targetUri)
                        .build());
            } else {
                String remoteHost = ((HttpServletRequest) request).getRemoteHost();
                String requestUri = ((HttpServletRequest) request).getRequestURI();
                String requestType = ((HttpServletRequest) request).getMethod();
                String targetUri = requestUri.concat("===").concat(requestType.toUpperCase());
                String userAgent = SurenessCommonUtil.findUserAgent((HttpServletRequest) request);
                subjectList.add(NoneSubject.builder().setRemoteHost(remoteHost)
                        .setTargetUri(targetUri)
                        .setUserAgent(userAgent).build());
            }
        }
        return subjectList;
    }

    @Override
    public void registerSubjectCreator(SubjectCreate creator) {

    }

    @Override
    public List<SubjectCreate> loadSubjectCreators() {
        return null;
    }
}