#!/usr/bin/env python3

import subprocess
import argparse
import atexit
import socket
import time

clientPORT = 13378
serverPORT = 13378

def initParser():
    parser = argparse.ArgumentParser(description="NFC Management")
    parser.add_argument("-adb", "--adb-path", action="store", type=str, dest="PATH", help="Path to ADB. Defaults to adb", default="adb")

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

def createConnection():

    TCP_IP = "localhost"
    TCP_PORT = serverPORT

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((TCP_IP, TCP_PORT))

    return s

def sendMessage(s, message):

    message = message.replace("\n", "").replace("\r","")

    BUFFER_SIZE = 10_000
    MESSAGE = message.encode("utf-8") + b"\n"

    s.send(MESSAGE)
    data = s.recv(BUFFER_SIZE)
    response = data.decode("utf-8")

    return(response)    

def main():

    s = createConnection()
    r = sendMessage(s, """
    
    {
        "getUID": "ALIHFUKASHDFYUHASDUKFHASKUDF",
        "readTag": "ASDASDASD"
    }
    
    """)
    print(r)

    s.close()



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
    config = initParser()
    main()
    