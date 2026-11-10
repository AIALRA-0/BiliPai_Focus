"""Offline QA contact sheet for the cleaning timeline (not an Android/Lottie renderer).
Samples its actual keyframes, masks and layer transforms using Pillow.
"""
import argparse
import base64
import io
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageChops

HERE = Path(__file__).parent
ROOT = HERE.parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--state', choices=['cleaning', 'retry', 'refreshing', 'follow_success', 'unfollow_complete', 'dislike_confirmed', 'share_ready', 'coin_success'], default='cleaning')
state = parser.parse_args().state
timeline_path = HERE/'refreshing-draft.json' if state=='refreshing' else ROOT/'brand-motion/src/main/res/raw'/f'bilipai_maid_{state}.json'
data = json.loads(timeline_path.read_text())
image_name = next(a['p'] for a in data['assets'] if 'p' in a)
image_source = io.BytesIO(base64.b64decode(image_name.split(',',1)[1])) if image_name.startswith('data:') else ROOT/'brand-motion/src/main/res/drawable-nodpi'/image_name
bitmap = Image.open(image_source).convert('RGBA')

def value(prop, frame):
    if not prop['a']: return prop['k']
    k=prop['k']
    for a,b in zip(k,k[1:]):
        if a['t'] <= frame < b['t']:
            t=(frame-a['t'])/(b['t']-a['t'])
            # Lottie's temporal cubic easing: solve x then evaluate y.
            lo,hi=0.,1.
            x1=a['o']['x'][0]; x2=a['i']['x'][0]
            y1=a['o']['y'][0]; y2=a['i']['y'][0]
            for _ in range(24):
                u=(lo+hi)/2
                x=3*(1-u)**2*u*x1+3*(1-u)*u*u*x2+u**3
                if x<t: lo=u
                else: hi=u
            u=(lo+hi)/2
            t=3*(1-u)**2*u*y1+3*(1-u)*u*u*y2+u**3
            return [s+(e-s)*t for s,e in zip(a['s'],b['s'])]
    return k[-1]['s']

def scalar(v): return v[0] if isinstance(v,list) else v

def matrix(layer,frame):
    ks=layer['ks']; a=value(ks['a'],frame);p=value(ks['p'],frame);s=value(ks['s'],frame)
    r=math.radians(scalar(value(ks['r'],frame)));c=math.cos(r);z=math.sin(r)
    m=np.array([[c*s[0]/100,-z*s[1]/100,0],[z*s[0]/100,c*s[1]/100,0],[0,0,1.]])
    m[:2,2]=np.array(p[:2])-m[:2,:2]@np.array(a[:2])
    if 'parent' in layer: m=matrix(next(l for l in data['layers'] if l['ind']==layer['parent']),frame)@m
    return m

def render(frame):
    canvas=Image.new('RGBA',(512,512))
    for layer in reversed(data['layers']):
        if layer['ty']==3: continue
        if 'refId' in layer:
            image=bitmap.copy();mask=Image.new('L',(512,512),0)
            for m in layer.get('masksProperties',[]):
                part=Image.new('L',(512,512),0);ImageDraw.Draw(part).polygon([tuple(p) for p in m['pt']['k']['v']],fill=255)
                mask=ImageChops.subtract(mask,part) if m['mode']=='s' else ImageChops.lighter(mask,part)
            image.putalpha(ImageChops.multiply(image.getchannel('A'),mask))
        else:
            image=Image.new('RGBA',(512,512));draw=ImageDraw.Draw(image)
            for g in layer['shapes']:
                fill=next((x for x in g['it'] if x['ty']=='fl'),None)
                stroke=next((x for x in g['it'] if x['ty']=='st'),None)
                for sh in g['it']:
                    if sh['ty']!='sh': continue
                    path=sh['ks']['k']; pts=[tuple(p) for p in path['v']]
                    if fill: draw.polygon(pts,fill=tuple(round(c*255) for c in fill['c']['k']))
                    if stroke:
                        # Sample cubic tangents rather than a sharp V-shaped eyelash.
                        curve=[]
                        for j in range(len(pts)-1):
                            start=np.array(pts[j]);stop=np.array(pts[j+1]);out=start+path['o'][j];inc=stop+path['i'][j+1]
                            for t in np.linspace(0,1,20): curve.append(tuple((1-t)**3*start+3*(1-t)**2*t*out+3*(1-t)*t*t*inc+t**3*stop))
                        draw.line(curve,fill=tuple(round(c*255) for c in stroke['c']['k']),width=2)
        inv=np.linalg.inv(matrix(layer,frame))
        image=image.transform((512,512),Image.Transform.AFFINE,tuple(inv[:2].flatten()),Image.Resampling.BICUBIC)
        opacity=scalar(value(layer['ks']['o'],frame))/100
        if opacity<1:image.putalpha(image.getchannel('A').point(lambda x:round(x*opacity)))
        canvas.alpha_composite(image)
    return canvas

frames={'cleaning':[0,24,48,66,72,95], 'retry':[0,15,30,42,49,89], 'refreshing':[0,21,42,50,63,83], 'follow_success':[0,20,39,44,50,71], 'unfollow_complete':[0,15,21,35,42,59], 'dislike_confirmed':[0,14,28,34,40,59], 'share_ready':[0,20,39,44,50,71], 'coin_success':[0,20,39,44,50,71]}[state]
sheet=Image.new('RGB',(256*6,290*2))
for row,bg in enumerate(['#FFFFFF','#142335']):
    for col,f in enumerate(frames):
        tile=Image.new('RGBA',(256,290),bg);tile.alpha_composite(render(f).resize((256,256),Image.Resampling.LANCZOS))
        ImageDraw.Draw(tile).text((12,264),f'Frame {f} / {data["op"]}',fill='#294960' if row==0 else '#E9F4FF')
        sheet.paste(tile.convert('RGB'),(col*256,row*290))
sheet.save(HERE/f'preview-{state}-frames.png')
print(f'Saved six sampled {state} poses on light and dark backgrounds.')
