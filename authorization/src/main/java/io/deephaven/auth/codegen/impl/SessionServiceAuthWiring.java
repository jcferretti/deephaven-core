//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.auth.codegen.impl;

import io.deephaven.auth.AuthContext;
import io.deephaven.auth.ServiceAuthWiring;
import io.deephaven.proto.backplane.grpc.ExportNotificationRequest;
import io.deephaven.proto.backplane.grpc.ExportRequest;
import io.deephaven.proto.backplane.grpc.HandshakeRequest;
import io.deephaven.proto.backplane.grpc.PublishRequest;
import io.deephaven.proto.backplane.grpc.ReleaseRequest;
import io.deephaven.proto.backplane.grpc.SessionServiceGrpc;
import io.deephaven.proto.backplane.grpc.TerminationNotificationRequest;
import io.grpc.ServerServiceDefinition;

/**
 * This interface provides type-safe authorization hooks for SessionServiceGrpc.
 */
public interface SessionServiceAuthWiring extends ServiceAuthWiring<SessionServiceGrpc.SessionServiceImplBase> {
    /**
     * Wrap the real implementation with authorization checks.
     *
     * @param delegate the real service implementation
     * @return the wrapped service implementation
     */
    default ServerServiceDefinition intercept(SessionServiceGrpc.SessionServiceImplBase delegate) {
        final ServerServiceDefinition service = delegate.bindService();
        final ServerServiceDefinition.Builder serviceBuilder =
                ServerServiceDefinition.builder(service.getServiceDescriptor());

        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "NewSession", null, this::onMessageReceivedNewSession));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "RefreshSessionToken", null, this::onMessageReceivedRefreshSessionToken));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "CloseSession", null, this::onMessageReceivedCloseSession));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "Release", null, this::onMessageReceivedRelease));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "ExportFromTicket", null, this::onMessageReceivedExportFromTicket));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "PublishFromTicket", null, this::onMessageReceivedPublishFromTicket));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "ExportNotifications", null, this::onMessageReceivedExportNotifications));
        serviceBuilder.addMethod(ServiceAuthWiring.intercept(
                service, "TerminationNotification", null, this::onMessageReceivedTerminationNotification));

        return serviceBuilder.build();
    }

    /**
     * Authorize a request to NewSession.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke NewSession
     */
    void onMessageReceivedNewSession(AuthContext authContext, HandshakeRequest request);

    /**
     * Authorize a request to RefreshSessionToken.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke RefreshSessionToken
     */
    void onMessageReceivedRefreshSessionToken(AuthContext authContext, HandshakeRequest request);

    /**
     * Authorize a request to CloseSession.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke CloseSession
     */
    void onMessageReceivedCloseSession(AuthContext authContext, HandshakeRequest request);

    /**
     * Authorize a request to Release.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke Release
     */
    void onMessageReceivedRelease(AuthContext authContext, ReleaseRequest request);

    /**
     * Authorize a request to ExportFromTicket.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke ExportFromTicket
     */
    void onMessageReceivedExportFromTicket(AuthContext authContext, ExportRequest request);

    /**
     * Authorize a request to PublishFromTicket.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke PublishFromTicket
     */
    void onMessageReceivedPublishFromTicket(AuthContext authContext, PublishRequest request);

    /**
     * Authorize a request to ExportNotifications.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke ExportNotifications
     */
    void onMessageReceivedExportNotifications(AuthContext authContext,
            ExportNotificationRequest request);

    /**
     * Authorize a request to TerminationNotification.
     *
     * @param authContext the authentication context of the request
     * @param request the request to authorize
     * @throws io.grpc.StatusRuntimeException if the user is not authorized to invoke TerminationNotification
     */
    void onMessageReceivedTerminationNotification(AuthContext authContext,
            TerminationNotificationRequest request);

    class AllowAll implements SessionServiceAuthWiring {
        public void onMessageReceivedNewSession(AuthContext authContext, HandshakeRequest request) {}

        public void onMessageReceivedRefreshSessionToken(AuthContext authContext,
                HandshakeRequest request) {}

        public void onMessageReceivedCloseSession(AuthContext authContext, HandshakeRequest request) {}

        public void onMessageReceivedRelease(AuthContext authContext, ReleaseRequest request) {}

        public void onMessageReceivedExportFromTicket(AuthContext authContext, ExportRequest request) {}

        public void onMessageReceivedPublishFromTicket(AuthContext authContext,
                PublishRequest request) {}

        public void onMessageReceivedExportNotifications(AuthContext authContext,
                ExportNotificationRequest request) {}

        public void onMessageReceivedTerminationNotification(AuthContext authContext,
                TerminationNotificationRequest request) {}
    }

    class DenyAll implements SessionServiceAuthWiring {
        public void onMessageReceivedNewSession(AuthContext authContext, HandshakeRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedRefreshSessionToken(AuthContext authContext,
                HandshakeRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedCloseSession(AuthContext authContext, HandshakeRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedRelease(AuthContext authContext, ReleaseRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedExportFromTicket(AuthContext authContext, ExportRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedPublishFromTicket(AuthContext authContext,
                PublishRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedExportNotifications(AuthContext authContext,
                ExportNotificationRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }

        public void onMessageReceivedTerminationNotification(AuthContext authContext,
                TerminationNotificationRequest request) {
            ServiceAuthWiring.operationNotAllowed();
        }
    }

    class TestUseOnly implements SessionServiceAuthWiring {
        public SessionServiceAuthWiring delegate;

        public void onMessageReceivedNewSession(AuthContext authContext, HandshakeRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedNewSession(authContext, request);
            }
        }

        public void onMessageReceivedRefreshSessionToken(AuthContext authContext,
                HandshakeRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedRefreshSessionToken(authContext, request);
            }
        }

        public void onMessageReceivedCloseSession(AuthContext authContext, HandshakeRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedCloseSession(authContext, request);
            }
        }

        public void onMessageReceivedRelease(AuthContext authContext, ReleaseRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedRelease(authContext, request);
            }
        }

        public void onMessageReceivedExportFromTicket(AuthContext authContext, ExportRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedExportFromTicket(authContext, request);
            }
        }

        public void onMessageReceivedPublishFromTicket(AuthContext authContext,
                PublishRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedPublishFromTicket(authContext, request);
            }
        }

        public void onMessageReceivedExportNotifications(AuthContext authContext,
                ExportNotificationRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedExportNotifications(authContext, request);
            }
        }

        public void onMessageReceivedTerminationNotification(AuthContext authContext,
                TerminationNotificationRequest request) {
            if (delegate != null) {
                delegate.onMessageReceivedTerminationNotification(authContext, request);
            }
        }
    }
}
