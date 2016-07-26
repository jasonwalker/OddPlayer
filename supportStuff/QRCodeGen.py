import qrcode
import qrcode.image.svg
from StringIO import StringIO

urls = [
    ('Startup', "http://feeds.hearstartup.com/hearstartup"),
    ('Back Story', "http://feeds.feedburner.com/BackStoryRadio"),
    ('Mystery Show', "http://feeds.gimletmedia.com/mysteryshow"),
    ('Serial', "http://feeds.serialpodcast.org/serialpodcast"),
    ('Planet Money', "http://www.npr.org/rss/podcast.php?id=510289"),
    ('This American Life', "http://feeds.thisamericanlife.org/talpodcast"),
    ('Invisibilia', "http://www.npr.org/rss/podcast.php?id=510307"),
    ('TED Radio Hour', "http://www.npr.org/rss/podcast.php?id=510298"),
    ('Damn Interesting', "http://feeds.feedburner.com/damn-interesting-podcast"),
    ('More or Less', "http://downloads.bbc.co.uk/podcasts/radio4/moreorless/rss.xml"),
    ('99 Percent Invisible', "http://feeds.99percentinvisible.org/99percentinvisible"),
    ('Reply All', "http://feeds.hearstartup.com/hearreplyall"),
    ('The Moth', "http://feeds.themoth.org/themothpodcast"),
    ('Freakonomics',"http://feeds.feedburner.com/freakonomicsradio"),
    ('Radio Diaries', "http://feed.radiodiaries.org/radio-diaries"),
    ('EconTalk', "http://econlib.org/library/EconTalk.xml"),
    ('Science Friday',"http://npr.org/rss/podcast.php?id=510221"),
    ('Commonwealth Club of California', "http://audio.commonwealthclub.org/audio/podcast/weekly.xml"),
    ('The Story Collider', "http://feeds.feedburner.com/TheStoryCollider"),
    ('RadioLab', "http://radiolab.org/feeds/podcast/"),
    ('Fugitive Waves', "http://feeds.fugitivewaves.org/FugitiveWaves"),
    ('The Economist', "http://feeds.feedburner.com/feedroom/BjnB"),
    ('The Memory Palace', "http://feeds.feedburner.com/TheMemoryPalace")
]
    
def main():
    factory = qrcode.image.svg.SvgPathFillImage
    html = '''\
<html>
    <style type="text/css">
        .img-with-text {
            text-align: center;
            border-style: solid;
            border-width: 1px;
            border-color: #334323;
        }

        .img-with-text img {
            display: block;
            margin: 0 auto;
        }
    </style>
    <title sty>Podcast QR Codes</title>
    <h1>Podcast QR Codes</h1>
    <body>\n'''

    for name, url in urls:
        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=18,
            border=4)
        qr.add_data(url)
        qr.make(fit=True)
        qrData = qr.make_image(image_factory=factory)
        output = StringIO()
        qrData.save(output)
        
        print type(qrData)
        html = html + '''\
        <div class="img-with-text">
            <p><h2>%s</h2></p>
            <div>%s</div>
            <p><h3><a href="%s">%s<a></h3></p>
        </div>\n''' % (name, output.getvalue().strip(), url, url)
    html =  html + '''\
    </body>
</html>'''
    with open('QRCodes.html','w') as f:
        f.write(html)
        
if __name__=='__main__':
    main()
    
