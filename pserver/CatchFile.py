#!/usr/bin/python
import socket,sys
from threading import Thread
BUFFER = 131072
class ConnectSockets(Thread):
    def __init__(self, input, listenSocket, name):
        Thread.__init__(self)
        self.input,self.listen,self.name = input,listenSocket,name
        self.daemon = True

    def run(self):
        try:
            with open('out.zip','wb') as outFile:
                while True:
                    print 'waiting'
                    data = self.input.recv(BUFFER)
                    
                    if data == '':
                        self.input.shutdown(2)
                        break
                    print data
                    outFile.write(data)
                    outFile.flush()
        finally:
            outFile.close()
            self.input.close()
            self.listen.close()


def main():
    print 'here1'
    listenSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print 'here2'
    listenSocket.bind(('', 9999))
    print 'here3'
    listenSocket.listen(5)
    print 'here4'

    (hereSocket, address) = listenSocket.accept()
    print 'here5'
    ConnectSockets(hereSocket,listenSocket, 'me --> them').start()
    print 'hit enter to exit program'
    sys.stdin.readline()
    
if __name__=='__main__':
    main()
        
