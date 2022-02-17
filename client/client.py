#!/usr/bin/env python3

import subprocess
import argparse
import atexit
import socket
import sys

clientPORT = 13378
serverPORT = 13378

def printHelp():
        print("""
        Either execute a command with 'do' or initialize a shell with 'shell'.
        See the readme for more information.
        """)

def parseCommands():
    commands = (sys.argv)[1:]

    mode = commands[0]

    if mode == "-h" or mode == "--help" or mode.lower() == "help":
        printHelp()
        exit()

    return(commands)


def initParser():
    parser = argparse.ArgumentParser(description="NFC Management")
    parser.add_argument("-adb", "--adb-path", action="store", type=str, dest="PATH", help="Path to ADB. Defaults to adb", default="adb")
    parser.add_argument("")

def adbCaller(command):
    command = command.split(" ")
    process = subprocess.run(command, capture_output=True)
    return process

def initADB():

    output = str(adbCaller("adb devices").stdout).split("\\n")
    devices = [x for x in output if "\\t" in x]
    if not devices:
        print("No devices attached. Check your USB connection and try again")
        exit()
    elif len(devices) > 1:
        print("More than one device attached. Disconnect other connected devices and try again")
        exit()
    else:
        device = devices[0].split("\\t")[0]
        print(f"Device with ID {device} found.")
        print(f"Binding local port {clientPORT} and server port {serverPORT}...")
        adbCaller(f"adb forward tcp:{clientPORT} tcp:{serverPORT}")

def getDump(path):
    return

def parseMessage(message):
    return

def createConnection():

    TCP_IP = "localhost"
    TCP_PORT = serverPORT
    TIMEOUT = 10

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(TIMEOUT)
    s.connect((TCP_IP, TCP_PORT))

    return s

def sendMessage(s, message):

    message = message.replace("\n", "").replace("\r","")

    BUFFER_SIZE = 10_000
    MESSAGE = message.encode("utf-8") + b"\n"

    try:
        s.send(MESSAGE)
        data = s.recv(BUFFER_SIZE)
        response = data.decode("utf-8")
        return(response)
    except Exception as e:
        print(f"Connection failed with error: {e}")
        return

def createMessage(commands):

    print(commands)
    #return ""



    message = ""
    if directive == "READ":
        if argument == "A":
            message = """
            {
                "readTag": "placeholder"
            }
            """
        elif argument == "S":
            sector = commands[2]
            message = f"""
            {{
                "readSector": "{sector}"
            }}
            """
        elif argument == "B":
            block = commands[2]
            message = f"""
            {{
                "readBlock": "{block}"
            }}
            """
    elif directive == "WRITE":
        if argument == "A":
            path = commands[2]
            data = getDump(path)
            message = f"""
            {{
                "writeDump": "{data}"
            }}
            """
        elif argument == "B":
            block = commands[2]
            data = commands[3]
            message = f"""
            {{
                "writeBlock": "{block}"
                "data": {data}
            }}
            """
    return(message)

def runCommand(s, command):

    directive = command[0].upper()
    arguments = command[1:]
    path = ""

    if "-f" in arguments:
        i = arguments.index("-f")
        path = arguments[i+1]

    message = createMessage(directive, arguments, path)
    print(message)
    if message == "":
        print("Unrecognized directive")
        printHelp()
        return
    r = sendMessage(s, message)
    data = parseMessage(r)
    if directive == "READ" and path:
        with open(path, "w") as f:
            f.write(data)
        return
    print(r)

def main():

    s = createConnection()
    commands = parseCommands()
    if commands[0] == "do":
        runCommand(s, commands[1:])
        s.close()
        exit()
    elif commands[0] == "shell":
        try:
            while True:
                command = input().split(" ")
                runCommand(s, command)
        except KeyboardInterrupt:
            s.close()
            exit()
    else:
        printHelp()
        exit()



def onExit():
    print("Removing socket binding..")
    output = (adbCaller(f"adb forward --remove tcp:{str(clientPORT)}").stderr).decode('utf-8')
    if output == "":
        print("Socket binding removed")
    else:
        print(f"{output}")

if __name__ == "__main__":
    atexit.register(onExit)
    initADB()
    main()
    