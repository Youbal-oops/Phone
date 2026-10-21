# Callrec Phone

A plain, private phone app for Android that **records your calls**. It replaces the dialer, shows who is calling, and saves each call as an audio file on your phone. No account, no ads, no tracking, no cloud: your calls, contacts and recordings never leave the device.

It is also the reference app for [**callrec**](https://github.com/Youbal-oops/callrec), the small open-source library that does the recording.

> **Not yet verified on many handsets.** Call-audio capture depends on the phone maker. Please report what works and what does not.

## What you get

- **Recents, dialpad and contacts**, with search that ignores case, accents and punctuation, and finds a number however it is written.
- **A full call screen**: answer, decline, silence the ringer, mute, speaker or Bluetooth, hold, keypad, end.
- **Saved names from everywhere**: Google, phone storage and SIM contacts are merged, and each name shows where it is saved.
- **A note per number**, shown on the call screen when that person calls or you call them.
- **Automatic call recording** once a call connects. Play, share or delete from the Recordings tab. Old recordings can delete themselves after 7, 30 or 90 days.
- **A built-in setup guide** (Settings → Setup) with a tick for each step, and a "how it works" list.
- **Problem reports stay on the phone.** If something breaks, Settings shows the error with a Copy button.

## Setup

The same five steps are in the app under **Settings → Setup**.

1. **Install and start Shizuku** (optional, but needed to record *both* sides of a call). Get it from <https://shizuku.rikka.app/download/>, then start it without root (Android 11+):
   1. Turn on **Developer options** (tap *Build number* seven times in *About phone*).
   2. Turn on **USB debugging**, then open **Wireless debugging** and turn it on.
   3. In Shizuku choose **Pairing**. In Wireless debugging tap **Pair device with pairing code** and type the code from Shizuku's notification.
   4. In Shizuku tap **Start**. If it fails, turn Wireless debugging off and on, then start again.
   5. Pairing is one time. **Start must be repeated after every reboot.**
   Android 10 or older: use the USB command in [Shizuku's guide](https://shizuku.rikka.app/guide/setup/).
   Without Shizuku the app records with the microphone, which hears the other person only on speakerphone.
2. **Allow restricted settings, then the permissions.** Phone Settings → Apps → Callrec Phone → ⋮ (top right) → **Allow restricted settings** (needed for apps installed from an APK). Then tap **Allow permissions** in the app (contacts, call log, phone, microphone, notifications).
3. **Set Callrec Phone as your default phone app.**
4. **Authorise it in Shizuku** when Shizuku asks. Recording then works on its own.
5. **Make a short test call.** The recording appears in the **Recordings** tab.

## Why Shizuku, and why not one app?

Android only lets *system* apps capture the other person's voice during a call. There is no setting to switch on and no permission to request. The options are root, the phone maker's own recorder, or a helper that borrows the shell's rights over *wireless debugging*. [Shizuku](https://shizuku.rikka.app) is that helper, and it is open source (Apache-2.0).

Shizuku is a separate app because Android does not let an app grant *itself* those rights: you have to start it from wireless debugging, and it must be restarted after a reboot. This app cannot include Shizuku and hide that step. What it does is detect which step you are on and tell you the next one.

A future version could do the wireless-debugging pairing itself, inside this app, so no second app is needed. That is a sizeable piece of work and needs testing on real phones; it is on the roadmap, not in this release.

## Privacy

- Everything stays on the phone. There is no server, no sign-in, no analytics and no network permission at all.
- Recordings are kept in the app's own folder (`Android/data/dev.youbal.callrecphone/files/Recordings`). Other apps cannot read them. Sharing is always an explicit action you take.
- Saved names are read from your contacts only to show and search them, and are not stored by the app.

## Recording calls and the law

Recording a call can require the consent of everyone on it, depending on where you live. **You are responsible for following the law.** Tell people when you record them.

## Build it yourself

You need JDK 17 and the Android SDK (API 35).

```
git clone https://github.com/Youbal-oops/Phone
cd Phone
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The recording library comes from JitPack. To work on the library and the app together, clone [callrec](https://github.com/Youbal-oops/callrec) **next to this folder**; the build uses that copy automatically.

## Roadmap

- Start Shizuku-style access from inside the app (wireless-debugging pairing), so no second app is needed.
- Optional on-device transcription.
- Voicemail and call blocking.

## Licence

MIT. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
