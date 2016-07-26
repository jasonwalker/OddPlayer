import web,os,sys
from web.wsgiserver import CherryPyWSGIServer
from web.httpserver import StaticMiddleware
from time import sleep
import socket, thread
import commands
import time
from newwords import c
PORT = 8080

FORMAT_STRING = "{:<20}{}"

urls = (
   '/favicon.ico', 'icon',
   '/(.*)', 'index'
)



def theTime():
    return time.strftime('%a, %d %b %Y %H:%m:%S %Z')


def getIPAddress():
    return commands.getoutput("/sbin/ifconfig eth0").split("\n")[1].split()[1][5:]

SIZE = 8 * 1024
ipAddress = getIPAddress()
def makeUrl(ipAddress, port):
    return "%s:%i" % (ipAddress, port)


with open('indexEpisode.html') as f:
    episodeHtml = f.read()

def getEpisodeText():
    fullText = ''
    for i in range(7):
        ident = c[int(time.time() * 1000000) % 10000]

        fullText = fullText + episodeHtml.format(address=makeUrl(ipAddress, PORT), episodeTitle="episodetitle_%s"%(ident,), publishDate=theTime(), 
            mp3Name="Lightsaber.mp3", episodeDescription="episodedescription_%s" % (ident,))
    return fullText

def getFeedText(requestPage):
    return mainHtml.format(address=makeUrl(ipAddress, PORT), feedTitle="feedtitle%s" % (requestPage,), feedDescription="feeddescription%s" % (requestPage,),\
                episodes=getEpisodeText())


web.pages = []
with open("indexMain.html") as f:
    mainHtml = f.read()

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
        if input.endswith(".mp3"): 
            web.header('Content-Type', 'audio/mpeg')
            with open(input,'rb') as f:
                while True:
                    val = f.read(SIZE)
                    if not val:
                        break
                    yield val
        else:
            yield getFeedText(input)
    
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
        run_simple(ipAddress, PORT, application, threaded=True, static_files={"/static":os.path.join(os.path.dirname(__file__),'static')})
    finally:
        print 'Stopped' 
    
def main():
    runServer()

if __name__=='__main__':
   main()
