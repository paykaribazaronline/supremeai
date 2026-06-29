export class AudioPlaybackService {
  private synth: SpeechSynthesis;
  private voice: SpeechSynthesisVoice | null = null;
  private audioContext: AudioContext;
  private analyser: AnalyserNode;

  constructor() {
    this.synth = window.speechSynthesis;
    // AudioContext used for Visualizer
    this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    this.analyser = this.audioContext.createAnalyser();
    this.analyser.fftSize = 256;
    
    // Load voices
    this.loadVoices();
    if (this.synth.onvoiceschanged !== undefined) {
      this.synth.onvoiceschanged = this.loadVoices.bind(this);
    }
  }

  private loadVoices() {
    const voices = this.synth.getVoices();
    // Try to find a robotic or British female voice for Aethel
    this.voice = voices.find(v => v.name.includes('Google UK English Female') || v.name.includes('Zira') || v.name.includes('Samantha')) || voices[0];
  }

  public getAnalyser(): AnalyserNode {
    return this.analyser;
  }

  public play(text: string) {
    if (this.synth.speaking) {
      console.warn('⚠️ [AudioPlaybackService] Already speaking, queueing...');
    }

    const utterance = new SpeechSynthesisUtterance(text);
    if (this.voice) {
      utterance.voice = this.voice;
    }
    
    // Cyber-Filter adjustments (Pitch & Rate) to simulate JARVIS/Aethel
    utterance.pitch = 0.8; // Slightly lower pitch
    utterance.rate = 1.1;  // Slightly faster

    // Note: True routing of SpeechSynthesis through Web Audio BiquadFilterNode 
    // is limited by browser security/APIs. We simulate the visualizer via a dummy oscillator 
    // while the speech is active to drive the WaveformVisualizer UI.
    
    let osc: OscillatorNode | null = null;

    utterance.onstart = () => {
      console.log('🗣️ [AudioPlaybackService] Aethel started speaking.');
      // Create a dummy oscillator to feed the analyser so the visualizer moves
      if (this.audioContext.state === 'suspended') {
        this.audioContext.resume();
      }
      osc = this.audioContext.createOscillator();
      const gain = this.audioContext.createGain();
      gain.gain.value = 0; // Silent oscillator, only used for data
      
      // Modulate oscillator frequency to make the waveform look like speech
      setInterval(() => {
        if (osc) osc.frequency.value = 100 + Math.random() * 400;
      }, 50);

      osc.connect(gain);
      gain.connect(this.analyser);
      this.analyser.connect(this.audioContext.destination);
      osc.start();
    };

    utterance.onend = () => {
      console.log('🛑 [AudioPlaybackService] Aethel finished speaking.');
      if (osc) {
        osc.stop();
        osc.disconnect();
        osc = null;
      }
    };

    this.synth.speak(utterance);
  }
}
