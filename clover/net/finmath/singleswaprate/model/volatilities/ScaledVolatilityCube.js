var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":100,"id":34401,"methods":[{"el":44,"sc":2,"sl":36},{"el":48,"sc":2,"sl":46},{"el":54,"sc":2,"sl":50},{"el":59,"sc":2,"sl":56},{"el":64,"sc":2,"sl":61},{"el":69,"sc":2,"sl":66},{"el":73,"sc":2,"sl":71},{"el":78,"sc":2,"sl":75},{"el":88,"sc":2,"sl":80},{"el":93,"sc":2,"sl":90},{"el":98,"sc":2,"sl":95}],"name":"ScaledVolatilityCube","sl":17}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_10":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testCalibration","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_342":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testMultiPiterbarg","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_439":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testStaticCubeCalibration","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_528":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"c_testMultiPiterbarg","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_600":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testCalibration","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_64":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testMulti","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]},"test_643":{"methods":[{"sl":36},{"sl":46},{"sl":50},{"sl":61},{"sl":66},{"sl":75},{"sl":90}],"name":"testSABRCubeCalibration","pass":true,"statements":[{"sl":37},{"sl":38},{"sl":39},{"sl":40},{"sl":41},{"sl":42},{"sl":43},{"sl":47},{"sl":53},{"sl":63},{"sl":68},{"sl":77},{"sl":92}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [], [], [10, 64, 643, 342, 600, 439, 528], [10, 64, 643, 342, 600, 439, 528], [], [], [10, 64, 643, 342, 600, 439, 528], [], [], [10, 64, 643, 342, 600, 439, 528], [], [], [], [], [], [], [], [10, 64, 643, 342, 600, 439, 528], [], [10, 64, 643, 342, 600, 439, 528], [], [], [10, 64, 643, 342, 600, 439, 528], [], [10, 64, 643, 342, 600, 439, 528], [], [], [], [], [], [], [10, 64, 643, 342, 600, 439, 528], [], [10, 64, 643, 342, 600, 439, 528], [], [], [], [], [], [], [], [], [], [], [], [], [10, 64, 643, 342, 600, 439, 528], [], [10, 64, 643, 342, 600, 439, 528], [], [], [], [], [], [], [], []]