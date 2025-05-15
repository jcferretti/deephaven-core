#
# Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
#
from typing import Any, Dict, Optional

from pydeephaven.dherror import DHError
from deephaven_core.proto import config_pb2, config_pb2_grpc


class ConfigService:
    def __init__(self, session):
        self.session = session
        self._grpc_app_stub = config_pb2_grpc.ConfigServiceStub(session.grpc_channel)

    def get_configuration_constants(self, timeout: Optional[float] = None, wait_for_ready = True) -> Dict[str, Any]:
        """Fetches the server configuration as a dict.
            Args:
                timeout (float): A timeout in seconds to use on the server call. Defaults to None which implies no timeout
                wait_for_ready (bool): If False, fail immediately if connection to server is down.  Defaults to True.
        """
        try:
            response = self.session.wrap_rpc(
                self._grpc_app_stub.GetConfigurationConstants,
                config_pb2.ConfigurationConstantsRequest(),
                timeout = timeout,
                wait_for_ready = wait_for_ready
            )
            return dict(response.config_values)
        except Exception as e:
            raise DHError("failed to get the configuration constants.") from e
