package com.glamrock.licenses.hystrix;

import com.glamrock.licenses.utils.UserContext;
import com.glamrock.licenses.utils.UserContextHolder;

import java.util.concurrent.Callable;

public final class DelegatingUserContextCallable<V> implements Callable<V> {
    private final Callable<V> delegate;
    private UserContext originalUserContext;

    public DelegatingUserContextCallable(Callable<V> delegate, UserContext userContext) {
        this.delegate = delegate;
        this.originalUserContext = userContext;
    }

    // @HystrixCommand 애너테이션이 proxy 역할로 메서드를 call 하기 전에 호출하는 함수
    @Override
    public V call() throws Exception {
        UserContextHolder.setContext(originalUserContext);

        try {
            // LicenseServer.getLicenseByOrg() 같은 히스트릭스가 proxy 하는 메서드의 call() 메서드를 호출
            return delegate.call();
        }
        finally {
            this.originalUserContext = null;
        }
    }

    public static <V> Callable<V> create(Callable<V> delegate,
                                         UserContext userContext) {
        return new DelegatingUserContextCallable<V>(delegate, userContext);
    }
}
