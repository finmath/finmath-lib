var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":371,"id":38463,"methods":[{"el":48,"sc":2,"sl":29},{"el":75,"sc":2,"sl":50},{"el":106,"sc":2,"sl":77},{"el":144,"sc":5,"sl":138},{"el":156,"sc":2,"sl":108},{"el":207,"sc":2,"sl":158},{"el":264,"sc":2,"sl":209},{"el":278,"sc":2,"sl":266},{"el":292,"sc":2,"sl":280},{"el":352,"sc":2,"sl":304},{"el":370,"sc":2,"sl":354}],"name":"AnalyticFormulasTest","sl":24}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_161":{"methods":[{"sl":50}],"name":"testBachelierOptionImpliedVolatility","pass":true,"statements":[{"sl":52},{"sl":53},{"sl":54},{"sl":55},{"sl":56},{"sl":58},{"sl":60},{"sl":61},{"sl":62},{"sl":63},{"sl":65},{"sl":66},{"sl":68},{"sl":71}]},"test_23":{"methods":[{"sl":304}],"name":"testBachelierRiskNeutralProbabilities","pass":true,"statements":[{"sl":306},{"sl":307},{"sl":308},{"sl":310},{"sl":311},{"sl":312},{"sl":313},{"sl":316},{"sl":318},{"sl":323},{"sl":325},{"sl":326},{"sl":328},{"sl":329},{"sl":330},{"sl":332},{"sl":333},{"sl":334},{"sl":336},{"sl":343},{"sl":344},{"sl":346},{"sl":347},{"sl":348}]},"test_245":{"methods":[{"sl":280}],"name":"testBlackScholesNegativeForward","pass":true,"statements":[{"sl":282},{"sl":283},{"sl":284},{"sl":285},{"sl":286},{"sl":288},{"sl":289},{"sl":291}]},"test_263":{"methods":[{"sl":266}],"name":"testBlackScholesPutCallParityATM","pass":true,"statements":[{"sl":268},{"sl":269},{"sl":270},{"sl":271},{"sl":272},{"sl":274},{"sl":275},{"sl":277}]},"test_28":{"methods":[{"sl":158}],"name":"testSABRSkewApproximation","pass":true,"statements":[{"sl":160},{"sl":161},{"sl":162},{"sl":163},{"sl":165},{"sl":166},{"sl":167},{"sl":168},{"sl":169},{"sl":170},{"sl":171},{"sl":172},{"sl":174},{"sl":175},{"sl":176},{"sl":177},{"sl":178},{"sl":179},{"sl":180},{"sl":181},{"sl":182},{"sl":184},{"sl":185},{"sl":186},{"sl":187},{"sl":188},{"sl":189},{"sl":190},{"sl":191},{"sl":193},{"sl":195},{"sl":197},{"sl":198},{"sl":199},{"sl":200},{"sl":202},{"sl":203},{"sl":205}]},"test_349":{"methods":[{"sl":29}],"name":"testBlackModelDigitalCapletDelta","pass":true,"statements":[{"sl":32},{"sl":33},{"sl":34},{"sl":35},{"sl":36},{"sl":37},{"sl":39},{"sl":41},{"sl":42},{"sl":43},{"sl":45},{"sl":47}]},"test_574":{"methods":[{"sl":209}],"name":"testSABRCurvatureApproximation","pass":true,"statements":[{"sl":212},{"sl":213},{"sl":214},{"sl":215},{"sl":217},{"sl":218},{"sl":219},{"sl":220},{"sl":221},{"sl":222},{"sl":223},{"sl":224},{"sl":226},{"sl":227},{"sl":228},{"sl":229},{"sl":230},{"sl":231},{"sl":232},{"sl":233},{"sl":234},{"sl":236},{"sl":237},{"sl":238},{"sl":239},{"sl":240},{"sl":241},{"sl":242},{"sl":243},{"sl":245},{"sl":248},{"sl":253},{"sl":254},{"sl":255},{"sl":256},{"sl":257},{"sl":259},{"sl":260},{"sl":262}]},"test_616":{"methods":[{"sl":354}],"name":"testVolatilityConversionLognormalToNormal","pass":true,"statements":[{"sl":357},{"sl":358},{"sl":359},{"sl":360},{"sl":361},{"sl":363},{"sl":366},{"sl":369}]},"test_632":{"methods":[{"sl":77}],"name":"testBachelierOptionDelta","pass":true,"statements":[{"sl":79},{"sl":80},{"sl":81},{"sl":82},{"sl":83},{"sl":85},{"sl":87},{"sl":88},{"sl":89},{"sl":90},{"sl":92},{"sl":94},{"sl":95},{"sl":96},{"sl":97},{"sl":99},{"sl":102}]},"test_69":{"methods":[{"sl":108},{"sl":138}],"name":"testSABRCalibration","pass":true,"statements":[{"sl":114},{"sl":115},{"sl":117},{"sl":118},{"sl":120},{"sl":121},{"sl":122},{"sl":123},{"sl":124},{"sl":127},{"sl":128},{"sl":129},{"sl":130},{"sl":132},{"sl":133},{"sl":134},{"sl":135},{"sl":140},{"sl":141},{"sl":142},{"sl":146},{"sl":148},{"sl":150},{"sl":152}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [349], [], [], [349], [349], [349], [349], [349], [349], [], [349], [], [349], [349], [349], [], [349], [], [349], [], [], [161], [], [161], [161], [161], [161], [161], [], [161], [], [161], [161], [161], [161], [], [161], [161], [], [161], [], [], [161], [], [], [], [], [], [632], [], [632], [632], [632], [632], [632], [], [632], [], [632], [632], [632], [632], [], [632], [], [632], [632], [632], [632], [], [632], [], [], [632], [], [], [], [], [], [69], [], [], [], [], [], [69], [69], [], [69], [69], [], [69], [69], [69], [69], [69], [], [], [69], [69], [69], [69], [], [69], [69], [69], [69], [], [], [69], [], [69], [69], [69], [], [], [], [69], [], [69], [], [69], [], [69], [], [], [], [], [], [28], [], [28], [28], [28], [28], [], [28], [28], [28], [28], [28], [28], [28], [28], [], [28], [28], [28], [28], [28], [28], [28], [28], [28], [], [28], [28], [28], [28], [28], [28], [28], [28], [], [28], [], [28], [], [28], [28], [28], [28], [], [28], [28], [], [28], [], [], [], [574], [], [], [574], [574], [574], [574], [], [574], [574], [574], [574], [574], [574], [574], [574], [], [574], [574], [574], [574], [574], [574], [574], [574], [574], [], [574], [574], [574], [574], [574], [574], [574], [574], [], [574], [], [], [574], [], [], [], [], [574], [574], [574], [574], [574], [], [574], [574], [], [574], [], [], [], [263], [], [263], [263], [263], [263], [263], [], [263], [263], [], [263], [], [], [245], [], [245], [245], [245], [245], [245], [], [245], [245], [], [245], [], [], [], [], [], [], [], [], [], [], [], [], [23], [], [23], [23], [23], [], [23], [23], [23], [23], [], [], [23], [], [23], [], [], [], [], [23], [], [23], [23], [], [23], [23], [23], [], [23], [23], [23], [], [23], [], [], [], [], [], [], [23], [23], [], [23], [23], [23], [], [], [], [], [], [616], [], [], [616], [616], [616], [616], [616], [], [616], [], [], [616], [], [], [616], [], []]