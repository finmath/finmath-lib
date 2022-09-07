var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":177,"id":27066,"methods":[{"el":95,"sc":2,"sl":47},{"el":157,"sc":2,"sl":107},{"el":170,"sc":2,"sl":168},{"el":175,"sc":2,"sl":172}],"name":"SwapLeg","sl":32}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_120":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testStochasticCurves","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":83},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_146":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testFloatLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_154":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testAgainstSwaption","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":83},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_198":{"methods":[{"sl":107},{"sl":172}],"name":"testFixLeg","pass":true,"statements":[{"sl":108},{"sl":109},{"sl":120},{"sl":121},{"sl":122},{"sl":124},{"sl":125},{"sl":126},{"sl":127},{"sl":128},{"sl":135},{"sl":139},{"sl":140},{"sl":148},{"sl":151},{"sl":153},{"sl":156},{"sl":174}]},"test_30":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testModelHierarchy","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_333":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testSwap","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":83},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_353":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testExpectedPositiveExposure","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":83},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_372":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_386":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testFixLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":83},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_428":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testCMSFloatLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_52":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testLIBORInArrearsFloatLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":79},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_632":{"methods":[{"sl":47},{"sl":168},{"sl":172}],"name":"testCMSSpreadLeg","pass":true,"statements":[{"sl":48},{"sl":50},{"sl":59},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":70},{"sl":74},{"sl":75},{"sl":76},{"sl":77},{"sl":86},{"sl":87},{"sl":89},{"sl":94},{"sl":169},{"sl":174}]},"test_70":{"methods":[{"sl":107},{"sl":172}],"name":"testFloatLeg","pass":true,"statements":[{"sl":108},{"sl":109},{"sl":120},{"sl":121},{"sl":122},{"sl":124},{"sl":125},{"sl":126},{"sl":127},{"sl":128},{"sl":135},{"sl":139},{"sl":140},{"sl":141},{"sl":144},{"sl":151},{"sl":153},{"sl":156},{"sl":174}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [], [], [], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 52, 428], [632], [], [146, 333, 30, 120, 353, 154, 372, 52, 428], [], [], [], [333, 120, 353, 154, 386], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [], [], [], [], [], [], [], [], [], [], [198, 70], [198, 70], [198, 70], [], [], [], [], [], [], [], [], [], [], [198, 70], [198, 70], [198, 70], [], [198, 70], [198, 70], [198, 70], [198, 70], [198, 70], [], [], [], [], [], [], [198, 70], [], [], [], [198, 70], [198, 70], [70], [], [], [70], [], [], [], [198], [], [], [198, 70], [], [198, 70], [], [], [198, 70], [], [], [], [], [], [], [], [], [], [], [], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [146, 333, 30, 120, 353, 154, 632, 372, 386, 52, 428], [], [], [146, 333, 198, 30, 120, 353, 70, 154, 632, 372, 386, 52, 428], [], [146, 333, 198, 30, 120, 353, 70, 154, 632, 372, 386, 52, 428], [], [], []]