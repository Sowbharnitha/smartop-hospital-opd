/**
 * SmartOP Global Queue & Polling Engine
 * Provides public OPD queue lookup and audio-visual cues on status changes.
 */

class QueueTracker {
    constructor(options = {}) {
        this.pollIntervalMs = options.pollIntervalMs || 5000;
        this.onUpdate = options.onUpdate || null;
        this.timer = null;
        this.lastStatus = null;
    }

    start(fetchFn) {
        this.stop();
        const poll = async () => {
            try {
                const data = await fetchFn();
                if (this.onUpdate) {
                    this.onUpdate(data);
                }
                this.checkStatusAlert(data);
            } catch (e) {
                console.warn("[QueueTracker] Polling cycle issue:", e.message);
            }
        };

        poll(); // Immediate first run
        this.timer = setInterval(poll, this.pollIntervalMs);
    }

    stop() {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    checkStatusAlert(data) {
        if (!data || !data.status) return;
        if (this.lastStatus && this.lastStatus !== data.status) {
            if (data.status === 'CALLED') {
                this.playCallNotification();
            }
        }
        this.lastStatus = data.status;
    }

    playCallNotification() {
        // Synthesize a soft hospital announcement tone using Web Audio API (Zero external audio assets)
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.type = 'sine';
            osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
            osc.frequency.setValueAtTime(880, ctx.currentTime + 0.2); // A5

            gain.gain.setValueAtTime(0.3, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.8);

            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + 0.8);
        } catch (e) {
            // AudioContext not allowed or unsupported
        }
    }
}
