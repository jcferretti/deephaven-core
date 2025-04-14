#
# Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
#
from typing import Any, Optional

from pydeephaven.dherror import DHError
from deephaven_core.proto import console_pb2_grpc, console_pb2
from pydeephaven.table import Table


class ConsoleService:
    def __init__(self, session):
        self.session = session
        self._grpc_console_stub = console_pb2_grpc.ConsoleServiceStub(session.grpc_channel)
        self.console_id = None

    def start_console(self, timeout: Optional[float] = None):
        """Starts a console session if one isn't already started.
            Args:
                timeout (float): A timeout in seconds to use on the server call. Defaults to None which implies no timeout
        """
        if self.console_id:
            return

        with self.session._r_lock:
            if not self.console_id:
                try:
                    result_id = self.session.make_export_ticket().pb_ticket
                    response = self.session.wrap_rpc(
                        self._grpc_console_stub.StartConsole,
                        console_pb2.StartConsoleRequest(
                            result_id=result_id,
                            session_type=self.session._session_type),
                        timeout = timeout,
                        wait_for_ready = True
                    )
                    self.console_id = response.result_id
                except Exception as e:
                    raise DHError("failed to start a console.") from e

    def run_script(self, server_script: str, systemic: Optional[bool] = None, timeout: Optional[float] = None) -> Any:
        """Runs a Python script in the console.
            Args:
                server_script (str): The script code to run
                systemic (bool): Whether to treat the code as systemically important. Defaults to None which uses the
                    default system behavior
                timeout (float): A timeout in seconds to use on the server call. Defaults to None which implies no timeout
        """
        self.start_console()

        try:
            systemic_opt = console_pb2.ExecuteCommandRequest.SystemicType.NOT_SET_SYSTEMIC if systemic is None else \
                console_pb2.ExecuteCommandRequest.SystemicType.EXECUTE_SYSTEMIC if systemic else \
                    console_pb2.ExecuteCommandRequest.SystemicType.EXECUTE_NOT_SYSTEMIC

            response = self.session.wrap_rpc(
                self._grpc_console_stub.ExecuteCommand,
                console_pb2.ExecuteCommandRequest(
                    console_id=self.console_id,
                    code=server_script,
                    systemic=systemic_opt),
                timeout = timeout,
                wait_for_ready = True
            )
            return response
        except Exception as e:
            raise DHError("failed to execute a command in the console.") from e

    def bind_table(self, table: Table, variable_name: str, timeout: Optional[float] = None):
        """Binds a name to an opened Table.
            Args:
                table (Table): A table
                variable_name (str): The variable name to bind the table to
                timeout (float): A timeout in seconds to use on the server call. Defaults to None which implies no timeout
        """
        if not table or not variable_name:
            raise DHError("invalid table and/or variable_name values.")
        try:
            self.session.wrap_rpc(
                self._grpc_console_stub.BindTableToVariable,
                console_pb2.BindTableToVariableRequest(
                    console_id=self.console_id,
                    table_id=table.pb_ticket,
                    variable_name=variable_name),
                timeout = timeout,
                wait_for_ready = True
            )
        except Exception as e:
            raise DHError("failed to bind a table to a variable on the server.") from e
