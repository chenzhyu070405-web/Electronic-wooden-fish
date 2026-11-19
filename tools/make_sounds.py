"""Synthesize the original mono cues. Requires numpy and an ffmpeg with libvorbis."""
import argparse
from pathlib import Path
import subprocess
import numpy as np

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/muyu/sounds"


def knock():
    t = np.arange(int(RATE*0.32))/RATE
    rng = np.random.default_rng(401)
    noise = rng.uniform(-1,1,t.size)
    body = sum(amp*np.sin(2*np.pi*freq*t)*np.exp(-t/decay) for freq,amp,decay in
               [(704,1,0.053),(1033,0.48,0.032),(1791,0.27,0.024),(2531,0.20,0.012)])
    return (body + np.diff(noise,prepend=0)*0.30*np.exp(-t/0.004)) * (1-np.exp(-t/0.0007))


def bowls(length, notes):
    output = np.zeros(round(length*RATE))
    for start, freq, gain, decay in notes:
        begin = int(start*RATE)
        t = np.arange(len(output)-begin)/RATE
        tone = np.zeros_like(t)
        for ratio, amp in [(1,1),(2.007,0.31),(2.719,0.16),(4.12,0.074)]:
            phase = 2*np.pi*freq*ratio*t
            tone += amp*(np.sin(phase)+0.20*np.sin(phase + 2*np.pi*0.72*t))*np.exp(-t*(ratio**0.7)/decay)
        output[begin:] += gain*tone*(1-np.exp(-t/0.012))
    # A short reflection and a tapered tail avoid clicks at the scene boundaries.
    lag = int(RATE*0.143)
    output[lag:] += 0.13*output[:-lag].copy()
    fade = min(len(output)//4,int(0.35*RATE))
    output[-fade:] *= np.linspace(1,0,fade)
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--ffmpeg", default="ffmpeg")
    args = parser.parse_args()
    ROOT.mkdir(parents=True, exist_ok=True)
    cues = {
        "knock": knock(),
        "omen": bowls(1.3, [(0,659.25,0.8,0.45),(0.11,880,0.42,0.38)]),
        "ascent": bowls(4.7, [(0,110,0.8,2.6),(0.32,164.81,0.45,2.8),(1.30,220,0.45,2.1),(2.1,329.63,0.4,1.5)]),
        "audience": bowls(4.6, [(0,174.61,0.8,2.7),(0.47,261.63,0.58,2.3),(1.18,349.23,0.38,2.0)]),
        "blessing": bowls(2.65, [(i*0.18,f,0.6,0.85) for i,f in enumerate([392,440,523.25,659.25,783.99])]),
        "passage": bowls(2.5, [(0,293.66,0.9,1.15),(0.27,146.83,0.5,1.0),(0.65,440,0.32,0.8)])}
    for name, signal in cues.items():
        pcm = (signal / max(abs(signal)) * 0.74).astype("<f4")
        subprocess.run([args.ffmpeg,"-v","error","-y","-f","f32le","-ar",str(RATE),"-ac","1",
                        "-i","pipe:0","-c:a","libvorbis","-q:a","5","-map_metadata","-1",str(ROOT/(name+".ogg"))],
                       input=pcm.tobytes(),check=True)
