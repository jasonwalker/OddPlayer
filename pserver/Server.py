import web,os,sys
from web.wsgiserver import CherryPyWSGIServer
from web.httpserver import StaticMiddleware
from time import sleep
import socket, thread
import commands

PORT = 8080

FORMAT_STRING = "{:<20}{}"

urls = (
   '/favicon.ico', 'icon',
   '/(.*)', 'index'
)

def getIPAddress():
    return commands.getoutput("/sbin/ifconfig").split("\n")[1].split()[1][5:]

SIZE = 8 * 1024
ipAddress = getIPAddress()
def makeUrl(ipAddress, port):
    return "%s:%i" % (ipAddress, port)

web.pages = []
for i in range(7):
    with open("index%i.html" % (i,)) as f:
        val = f.read()
        web.pages.append(val.format(address=makeUrl(ipAddress, PORT)))

class icon:
    def GET(self):
        return ''

class index:
    def pprint(self):
        env = web.ctx.environ
        print "-----------------------------------"
        for key in ['REQUEST_METHOD','PATH_INFO','QUERY_STRING']:
            val = env.get(key)
            if val:
                print FORMAT_STRING.format(key, env.get(key))
        input = web.input()
        keys = input.keys()
        if keys:
            print FORMAT_STRING.format("***KEY", "VALUE****")
            for key in keys:
                print FORMAT_STRING.format(key, input.get(key))
        if web.data():
            print "START RAW DATA****\n"
            print web.data()
            print "\nEND RAW DATA****"
        print '\n'

    def GET(self, input):
        if input.endswith(".mp3") or input.endswith(".ogg"): 
            web.header('Content-Type', 'audio/mpeg')
            with open(input,'rb') as f:
                while True:
                    val = f.read(SIZE)
                    if not val:
                        break
                    yield val
        else:
            yield web.pages[int(input)]
    
    def POST(self, input):
        self.pprint()
        return ''
    def PUT(self, input):
        self.pprint()
        return ''
    def DELETE(self, input):
        self.pprint()
        return ''
    def HEAD(self, input):
        self.pprint()
        return ''
    def TRACE(self, input):
        self.pprint()
        return ''
    def CONNECT(self, input):
        self.pprint()
        return ''

def runServer():
    app = web.application(urls, globals(), autoreload=True)
    application = StaticMiddleware(app.wsgifunc())
    from werkzeug.serving import run_simple
    print makeUrl(ipAddress, PORT)
    try:
        run_simple(ipAddress, PORT, application,
            threaded=True,
            static_files={"/static":os.path.join(os.path.dirname(__file__),'static')})
    finally:
        print 'Stopped' 
    
def main():
    runServer()

if __name__=='__main__':
   main()
