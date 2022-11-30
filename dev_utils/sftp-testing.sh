#!/bin/sh

mykey=$(pwd)/src/test/resources/id_ed25519
remusr=dummy
remhost=localhost
port=2222
tmpfile=$(pwd)/README.md

cleanup() {
  rm -f ${tmpfile}
}
trap cleanup 0

sftp -i $mykey -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o KbdInteractiveAuthentication=no -P ${port} ${remusr}@${remhost} <<EOF 
  put ${tmpfile}
  dir
  ls -al
  exit
EOF
ST=$?

if test $ST -ne 0
then
  echo SFTP LOGIN FAILURE. RC=${ST} 1>&2
  exit $ST
fi

exit $ST
