#!/bin/bash
# -d 1 [--download]
# -r [--restart]
baseUrl="https://dev.applab.cz/deploy/"
versions="versionsTony.php"

declare -A apps
apps[tony]="-Dhttp.port=9405 -DtonyServerId=11 -J-XX:+HeapDumpOnOutOfMemoryError -J-Xmx4g -J-Xms256m -Duser.timezone=Europe/Prague"

advancedWgetParams="--user=cmsdeploy --password=D3pl0y1ngCM5="

#if we need other path to load default java, we neet to add it to path to the first position
#PATH=/home/hrusak/jre/bin:$PATH
counter=0

numberOfApps=0

for i in "${!apps[@]}"; do
	numberOfApps=$[$numberOfApps +1]
done

prefix="tony-"
suffix=".zip"

versionPattern="*.*.*-????-??-??_??-??-??"
versionsUrl=$baseUrl$versions
data=$(wget "$versionsUrl" $advancedWgetParams -q -O -)

ABSOLUTE_PATH=$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)/`basename "${BASH_SOURCE[0]}"`
scriptDir=`dirname "$ABSOLUTE_PATH"`/
confDir=${scriptDir}conf/
nohupDir=${scriptDir}nout/
pidDir=${scriptDir}pid/

cd $scriptDir

echo "Deleting unnecesary files and directories, if they exist."
rm -Rf $scriptDir$prefix$versionPattern
rm -f $scriptDir$prefix$versionPattern$suffix

cd $scriptDir

if [ $numberOfApps -ge 1 ]; then
        echo "Last deployed version: " `cat lastDeployed | tail -n 1`
	echo "Available Tony versions:"
	echo "-1. Do nothing and skip..."
	echo "0. Do not download only restart"
	declare -A lines
	while IFS= read -r line
	do
		counter=$[$counter +1]
		echo "$counter. $line"
		lines[$counter]="$line"
	done <<< "$data"
	if [ "$1" = "-d" ]; then
		# download
		selection=$2
	else
		# restart
		selection=0
	fi
	if [ $selection -ge 0 ] && [ $selection -le $counter ]; then
		if [ $selection -ge 1 ]; then
			wget "$baseUrl$prefix${lines[$selection]}$suffix" $advancedWgetParams -O $prefix${lines[$selection]}$suffix
			unzip -q "$prefix${lines[$selection]}$suffix"
                        truncated=`echo ${lines[$selection]} |sed 's/-dev//g'`
                        echo "${lines[$selection]}" >> lastDeployed
		fi
		echo 
		echo "Killing running applications if any running."
		for i in "${!apps[@]}"; do
			echo ${pidDir}${i}.pid
			if [ -f ${pidDir}${i}.pid ]; then
				echo "Killing old $i - pid: " `cat ${pidDir}${i}.pid`
				kill `cat ${pidDir}${i}.pid`
				while [ -f ${pidDir}${i}.pid  ]; do
					echo .
					sleep 1
				done
				echo "$i succesfully shutted down."
			fi
		done
		echo
		if [ $selection -ge 1 ]; then
			echo "Deleting old tony app."
			if [ -d "tony" ]; then
				rm -Rf "tony"
			fi
			echo
			echo "Moving new version to tony."
			mv "$prefix$truncated" "tony"
			rm "$prefix${lines[$selection]}$suffix"
		fi
		if ! [ -d ${pidDir} ]; then
			mkdir -p "${pidDir}"
		fi
		if ! [ -d ${nohupDir} ]; then
			mkdir -p "${nohupDir}"
		fi
		cd tony/bin
		for i in "${!apps[@]}"; do
			echo starting $i with nohup.
			#nohup bash tony -Dorg.freemarker.loggerLibrary=SLF4J -Dproduction.server=false -Dfile.enconding=UTF-8 -Dconfig.file=${confDir}${i}.conf ${apps[$i]} -Dlogger.file=${scriptDir}logger.xml -Dpidfile.path=${pidDir}${i}.pid -Dlogs.directory=${scriptDir}logs/${i} >> ${nohupDir}${i}.out &
		done
	else
		echo "Skipping tony downloading/restarting"
	fi
fi
