Engine_buftest4 : CroneEngine {

    var buffers;
    var synths;

    *new { arg context, doneCallback;
        ^super.new(context, doneCallback);
    }

    alloc {
    
    SynthDef("bufplayer", {
      arg out=0, rate=1, start=0, end=1, trig=1, amp=1, buf, reset=0;
      var snd,snd2,pos,pos2,frames,duration,env;
	    var startA,endA,startB,endB,resetA,resetB,crossfade,aOrB;
	    
	    // latch to change trigger between the two
    	aOrB=ToggleFF.kr(trig);
    	startA=Latch.kr(start,aOrB);
    	endA=Latch.kr(end,aOrB);
    	resetA=Latch.kr(reset,aOrB);
    	startB=Latch.kr(start,1-aOrB);
    	endB=Latch.kr(end,1-aOrB);
    	resetB=Latch.kr(reset,1-aOrB);
    	crossfade=Lag.ar(K2A.ar(aOrB),0.05);
      
      // rate is modified by BufRateScale to convert between sampling rates
	    rate = rate*BufRateScale.kr(buf);
	    // frames is the number of frames
	    frames = BufFrames.kr(buf);
	    
	    // Phasor is a ramp
	    pos=Phasor.ar(
  		trig:aOrB,
  		rate:rate,
  		start:(((rate>0)*startA)+((rate<0)*endA))*frames,
  		end:(((rate>0)*endA)+((rate<0)*startA))*frames,
  		resetPos:(((rate>0)*resetA)+((rate<0)*endA))*frames,
	    );
	    
	    snd=BufRd.ar(
		    numChannels:2,
		    bufnum:buf,
		    phase:pos,
		    loop:0,
		    interpolation:4,
	    );
	    
	    // add a second reader
    	pos2=Phasor.ar(
  		trig:(1-aOrB),
  		rate:rate,
  		start:(((rate>0)*startB)+((rate<0)*endB))*frames,
  		end:(((rate>0)*endB)+((rate<0)*startB))*frames,
  		resetPos:(((rate>0)*resetB)+((rate<0)*endB))*frames,
    	);
    	
    	snd2=BufRd.ar(
  		numChannels:2,
  		bufnum:buf,
  		phase:pos2,
  		interpolation:4,
    	);
	    
	    env=EnvGen.ar(Env.asr(0.01,1,0.01,0),gate:trig,doneAction:2);
	    
  	  Out.ar(out,(crossfade*snd)+((1-crossfade)*snd2) * env * amp)
      }).add;
    
    context.server.sync;
    
    synths=Array.newClear(64);
    buffers=Array.newClear(32);
       
    this.addCommand("file", "is", { arg msg;
         var newbuf;
         var oldbuf;
         newbuf=Buffer.read(context.server, msg[2]);
         if(buffers[msg[1]].notNil, {
            oldbuf = buffers[msg[1]];
            buffers[msg[1]] = newbuf;
            oldbuf.free;
            },{
            buffers[msg[1]] = newbuf;
            });
         });
         
    this.addCommand("play", "ii", { arg msg;
        if(msg[2]==1,{
          synths[msg[1]]=Synth.new("bufplayer",
          [\buf,buffers[msg[1]]],
          target:context.server);
        });
        if(msg[2]==0,{
          synths[msg[1]].set(\trig,0);
        })});
        
    }

    free {
        synths.free;
    }
}
