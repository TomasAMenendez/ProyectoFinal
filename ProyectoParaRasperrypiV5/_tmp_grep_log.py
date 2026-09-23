import os
import sys

import paramiko

path = sys.argv[1] if len(sys.argv) > 1 else (
    "/home/teo/Desktop/ProyectoParaRasperrypiV5/logs/sistema_2026-09-23_11-30-17.txt"
)
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect(
    os.environ.get("PI_HOST", "192.168.0.148"),
    username=os.environ.get("PI_USER", "teo"),
    password=os.environ["PI_SSH_PASS"],
    timeout=15,
)
cmd = (
    "grep -nE 'TRANSCRIPTION|STT_FILTER|adios|Adios|hola|Hola|farewell|greeting|INTENT|NLU' "
    + path
    + " | head -120"
)
_, stdout, stderr = c.exec_command(cmd)
sys.stdout.write(stdout.read().decode("utf-8", "replace"))
sys.stderr.write(stderr.read().decode("utf-8", "replace"))
c.close()
