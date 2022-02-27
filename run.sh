#!/usr/bin/env bash

source ./setup.sh

function show_usage (){
    printf "Usage: $0 [options [parameters]]\n"
    printf "\n"
    printf "Options:\n"
    printf " -e|--env, valid values ( dev2, stage, prod )  [Optional, Default is dev2]\n"
    printf " -c|--cmd, valid commands ( setup, auth, importusers, deleteusers, changepwd) [Optional, Default is auth]\n"
    printf " -u|--user, User name\n"
    printf " -p|--pwd, Password \n"
    printf " -r|--role, Valid roles (dev, admin) [Optional, Default is dev]\n"
    printf " -t|--time, Session expiration time in hours [Optional, Default is 4 hrs]\n"
    printf " -f|--file, Csvfile for import/delete action [ Optional]\n"
	  printf " -np|--newpwd, New password [ Optional]\n"
    printf " -h|--help, Print help \n"

return 0
}

function show_error() {
    printf "User name and Password are required fields\n"
return 1
}
# if less than three arguments supplied, display usage
	if [  $# -le 0 ]
	then
		show_usage
		exit 1
	fi

# check whether user had supplied -h or --help . If yes display usage
	if [[ ( $# == "--help") ||  $# == "-h" ]]
	then
		display_usage
		exit 0
	fi

while [ ! -z "$1" ]; do
  case "$1" in
     --env|-e)
         shift
         ENV_ARG=$1
         ;;
     --cmd|-c)
         shift
         COMMAND_ARG=$1
         ;;
     --user|-u)
         shift
         USERNAME_ARG=$1
         ;;
     --pwd|-p)
        shift
        PASSWORD_ARG=$1
         ;;
    --role|-r)
        shift
        ROLE_ARG=$1
         ;;
    --time|-t)
        shift
        TIME_ARG=$1
         ;;
    --file|-f)
        shift
        CSVFFILE_ARG=$1
         ;;
    --newpwd|-np)
        shift
        NEWPWD_ARG=$1
         ;;
     *)
        show_usage
        ;;
  esac
shift
done

if [[ -z $USERNAME_ARG || -z  $PASSWORD_ARG ]]
then
    show_error
    exit 1
fi
if [[ -z $ENV_ARG ]]
then
    ENV_ARG=dev2
fi

export FEDERATE_VERSION="1.0.2-SNAPSHOT"

export JAVA_OPTS="-Dfederate.log.level=${FEDERATE_LOG_LEVEL}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.version=${FEDERATE_VERSION}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.region=${FEDERATE_REGION}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.env=${FEDERATE_ENV}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.org=${FEDERATE_ORG}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.keystore.path=${FEDERATE_JKS_PATH}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.store.password=${FEDERATE_STORE_PWD}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.key.password=${FEDERATE_KEY_PWD}"
export JAVA_OPTS="${JAVA_OPTS} -Dfederate.aws.prop=${FEDERATE_AWS_PROP}"

export JAVA_ARGS="uname=${USERNAME_ARG} pwd=${PASSWORD_ARG}"

if [[ -z $COMMAND_ARG ]]
then
    COMMAND_ARG=auth
fi
export JAVA_ARGS="${JAVA_ARGS} cmd=${COMMAND_ARG}"

if [[ -z $ROLE_ARG ]]
then
    ROLE_ARG=dev
fi
export JAVA_ARGS="${JAVA_ARGS} role=${ROLE_ARG}"

if [[ -z $TIME_ARG ]]
then
    TIME_ARG=4
fi
export JAVA_ARGS="${JAVA_ARGS} time=${TIME_ARG}"

if [[ ! -z $CSVFFILE_ARG ]]
then
    export JAVA_ARGS="${JAVA_ARGS} csvfile=${CSVFFILE_ARG}"
fi

if [[ ! -z $NEWPWD_ARG ]]
then
    export JAVA_ARGS="${JAVA_ARGS} newpwd=${NEWPWD_ARG}"
fi

echo "JAVA_OPTS : $JAVA_OPTS"
echo "JAVA_ARGS : $JAVA_ARGS"

java $JAVA_OPTS -jar federate-$FEDERATE_VERSION.jar $JAVA_ARGS
