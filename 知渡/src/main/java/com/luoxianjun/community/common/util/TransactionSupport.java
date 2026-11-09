package com.luoxianjun.community.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 把「事务提交后才该发生」的动作收敛到一处。
 *
 * <p>通知、消息这类副作用如果写在 @Transactional 方法内，事务回滚后副作用已经发出，
 * 会留下幽灵通知。没有事务时（例如单测直接调用）退化为立即执行，保证行为不丢。</p>
 */
public final class TransactionSupport {

    private TransactionSupport() {
    }

    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
