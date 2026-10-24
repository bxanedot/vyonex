import VyonEx
// bxane

func checkVyonExExtraction(host: VyonExHost) {
    let extractor = VyonEx(host: host)
    extractor.initialize()
    extractor.initializeFallback()
    extractor.audioCandidates(videoId: "dQw4w9WgXcQ") { _, _ in }
    extractor.extract(
        videoId: "dQw4w9WgXcQ",
        maxKbps: 128,
        skipClients: [],
        requireM4a: false
    ) { _, _ in }
}
