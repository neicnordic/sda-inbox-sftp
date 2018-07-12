# SFTP Inbox for LocalEGA

[![Coverage Status](https://coveralls.io/repos/github/NBISweden/LocalEGA-inbox/badge.svg)](https://coveralls.io/github/NBISweden/LocalEGA-inbox)

##LocalEGA login system

Central EGA contains a database of users, with IDs and passwords.

We have developed a solution based on [Apache Mina SSHD](https://mina.apache.org/sshd-project/)
to allow user authentication via
either a password or an RSA key against the CentralEGA database
itself. The user is locked within their home folder, which is done programmatically using [RootedFileSystem](https://github.com/apache/mina-sshd/blob/master/sshd-core/src/main/java/org/apache/sshd/common/file/root/RootedFileSystem.java).

The solution uses CentralEGA's user IDs but can also be extended to
use Elixir IDs (of which we strip the ``@elixir-europe.org`` suffix).


The procedure is as follows. The inbox is started without any created
user. When a user wants to log into the inbox (actually, only ``sftp``
uploads are allowed), the code looks up the username in a local
cache, and, if not found, queries the CentralEGA REST endpoint. Upon
return, we store the user credentials in the local cache and create
the user's home directory. The user now gets logged in if the password
or public key authentication succeeds. Upon subsequent login attempts,
only the local cache is queried, until the user's credentials
expire. The cache has a default TTL of one hour, and is wiped clean
upon reboot (as a cache should). Default TTL can be configured via ``CACHE_TTL`` env var.

The user's home directory is created when its credentials upon successful login.
Moreover, for each user, we detect when the file upload is completed and compute its
checksum. 

##Configuration

Environment variables used:


| Variable name    | Default value |
|------------------|---------------|
| BROKER_USERNAME  | guest         |
| BROKER_PASSWORD  | guest         |
| BROKER_HOST      | mq            |
| BROKER_PORT      | 5672          |
| INBOX_PORT       | 2222          |
| INBOX_LOCATION   | /ega/inbox/   |
| CACHE_TTL        | 3600.0        |
| CEGA_ENDPOINT    |               |
| CACHE_TTL        |               |

