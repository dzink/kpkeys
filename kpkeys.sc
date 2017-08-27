(play {
	arg impulseTone = 11.25, impulseGain = 0, oscillatorBrightness = 30, oscillatorDecay = 4, oscillatorRelease = 0.5, oscillatorGain = 1, oscillatorFeedback = 0.000001, tremDepth = 0, tremShape = 100, tremSpeed = 8, tremStutter = 1.5, tremSpin = 1.5, chorusDepth = 0.3, chorusSpeed = 2, chorusStutter = 1.25, chorusSpin = 1.25, chorusSec = 0.0000051, reverbMix, reverbSize, panSpin, bassBoost, preGain = 0.1, postGain = 0.5, panNote = 0.3;
	var audio, imp, index, pink, trem, chorus, chorusSum = 0, feedback;
	var chorusStages = 8;
	oscillatorBrightness = MouseX.kr(1, 40, 1);
	audio = 0;
	feedback = LocalIn.ar(2) * oscillatorFeedback;
	imp = Dust.kr(3);
	index = Stepper.kr(imp, 0, 0, 10, 1);
	pink = {PinkNoise.ar!2}!2 * 0.1;
	8.do {
		arg i;
		var note, iImp, voice, click, freq, iFeedback;
		var vel = 1;//MouseY.kr(0, 1);
		note = i * 7 % 32 + 42;
		iImp = imp * BinaryOpUGen('==', i, index);
		iImp = Trig1.kr(iImp, 0.02).lagud(0.02, vel.linexp(0, 1, 0.01, 0.07)) * vel;
		iFeedback = feedback * Trig1.kr(iImp, 1).lagud(0, oscillatorRelease);
		//iImp = CombC.kr(iImp, 0.1, 6.reciprocal, 3);
		freq = note.midicps;// * LFSaw.kr(0.1).range(1, 2);
		impulseTone = vel.linexp(0, 1, 0.25, 16);
		click = RLPF.ar(iImp * pink, freq * impulseTone + 600, 0.95, mul: 100);

		//freq = freq * Array.fill(
		voice = Mix.ar(CombC.ar(click + iFeedback, 0.1, freq.reciprocal - SampleDur.ir, oscillatorDecay, mul: oscillatorGain));
		voice.size.postln;
		voice = DelayN.ar(voice, 0.15, 0.05);
	    //voice = RLPF.ar(voice, freq * 7.5, 0.7);
		//oscillatorBrightness = oscillatorBrightness * LFPulse.kr(LFNoise1.kr(2!2).range(0.5, 4)).linexp(-1, 1, 0.25, 0.125.reciprocal);
		//click = BPF.ar(click, 200, 0.3);
		4.do {
			arg i;
			voice = RLPF.ar(voice, min(10000, freq * oscillatorBrightness), 1.713)  + (click * impulseGain);
		};
		//voice = voice * SelectX.kr(panNote, [1, LinPan2.kr(1, note.linlin(40, 70, -1, 1))]);

		audio = audio + LeakDC.ar(voice);
	};
	audio.size.postln;
	audio = (LeakDC.ar(audio) * preGain).softclip;


	tremSpeed = [tremSpeed, tremSpin * tremSpeed];
	trem = SinOsc.kr(LFNoise1.ar(2!2).range(tremStutter.reciprocal, tremStutter) * tremSpeed, [0, pi]);
	trem = trem.range(tremShape.neg, tremShape).softclip.range(1 - tremDepth, 1);
	audio = audio * trem;

	chorusSpeed = [chorusSpeed, chorusSpin * chorusSpeed];

	chorusDepth = 1 * LFPulse.kr(0.25).poll;
	chorusSec = MouseY.kr(0.00001, 0.001, 1, 0.01).poll;
	chorusSum =0;
	chorusStages.do {
		arg i;
		chorus = SinOsc.kr(LFNoise1.ar(2!2).range(chorusStutter.reciprocal, chorusStutter) * chorusSpeed, [0, pi]);
		chorus = chorus.range(0, chorusSec) + 0.001;
		chorusSum = chorusSum + (AllpassC.ar(HPF.ar(audio, 20), 0.2, chorus * (i + 1) + 0, decay: 0.01) * chorusDepth);
	};
	audio = audio + (LeakDC.ar(chorusSum) / (1 + (max(0.001, chorusDepth) * 0.5 * chorusStages)));

	audio = audio + RLPF.ar(audio, 120, 3, bassBoost);// + 1;
	//audio = LeakDC.ar(Select.ar(BinaryOpUGen('>', audio, 0), [DC.ar(0), audio]));
	//audio = CompanderD.ar(audio, 0.5, 1, 0.1);
	//audio = audio + CombC.ar(audio, 0.2, 0.2, 3, 0.5);
	audio = FreeVerb.ar(audio, 0.5, 0.17);

	LocalOut.ar(audio);


	audio.softclip * postGain;
})