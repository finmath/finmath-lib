var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":220,"id":10023,"methods":[{"el":32,"sc":2,"sl":30},{"el":57,"sc":2,"sl":34}],"name":"AssetModelFourierMethodFactory","sl":25},{"el":102,"id":10048,"methods":[{"el":90,"sc":3,"sl":81},{"el":95,"sc":3,"sl":92},{"el":101,"sc":3,"sl":97}],"name":"AssetModelFourierMethodFactory.BlackScholesModelFourier","sl":70},{"el":146,"id":10056,"methods":[{"el":134,"sc":3,"sl":121},{"el":139,"sc":3,"sl":136},{"el":145,"sc":3,"sl":141}],"name":"AssetModelFourierMethodFactory.HestonModelFourier","sl":111},{"el":183,"id":10064,"methods":[{"el":171,"sc":3,"sl":159},{"el":175,"sc":3,"sl":172},{"el":181,"sc":3,"sl":177}],"name":"AssetModelFourierMethodFactory.MertonModelFourier","sl":154},{"el":219,"id":10072,"methods":[{"el":207,"sc":3,"sl":196},{"el":212,"sc":3,"sl":209},{"el":217,"sc":3,"sl":213}],"name":"AssetModelFourierMethodFactory.VarianceGammaModelFourier","sl":191}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_19":{"methods":[{"sl":30},{"sl":34},{"sl":159},{"sl":177}],"name":"test","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":41},{"sl":45},{"sl":46},{"sl":47},{"sl":160},{"sl":169},{"sl":170},{"sl":180}]},"test_328":{"methods":[{"sl":30},{"sl":34},{"sl":196},{"sl":213}],"name":"test","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":41},{"sl":45},{"sl":49},{"sl":50},{"sl":51},{"sl":197},{"sl":205},{"sl":206},{"sl":216}]},"test_403":{"methods":[{"sl":30},{"sl":34},{"sl":81},{"sl":97}],"name":"test","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":38},{"sl":39},{"sl":82},{"sl":88},{"sl":89},{"sl":100}]},"test_485":{"methods":[{"sl":30},{"sl":34},{"sl":121},{"sl":141}],"name":"test","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":41},{"sl":42},{"sl":43},{"sl":122},{"sl":132},{"sl":133},{"sl":144}]},"test_521":{"methods":[{"sl":30},{"sl":34},{"sl":121},{"sl":141}],"name":"hTest","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":41},{"sl":42},{"sl":43},{"sl":122},{"sl":132},{"sl":133},{"sl":144}]},"test_535":{"methods":[{"sl":30},{"sl":34},{"sl":81},{"sl":97}],"name":"bsTest","pass":true,"statements":[{"sl":31},{"sl":37},{"sl":38},{"sl":39},{"sl":82},{"sl":88},{"sl":89},{"sl":100}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [328, 485, 403, 19, 535, 521], [328, 485, 403, 19, 535, 521], [], [], [328, 485, 403, 19, 535, 521], [], [], [328, 485, 403, 19, 535, 521], [403, 535], [403, 535], [], [328, 485, 19, 521], [485, 521], [485, 521], [], [328, 19], [19], [19], [], [328], [328], [328], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [403, 535], [403, 535], [], [], [], [], [], [403, 535], [403, 535], [], [], [], [], [], [], [], [403, 535], [], [], [403, 535], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [485, 521], [485, 521], [], [], [], [], [], [], [], [], [], [485, 521], [485, 521], [], [], [], [], [], [], [], [485, 521], [], [], [485, 521], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [19], [19], [], [], [], [], [], [], [], [], [19], [19], [], [], [], [], [], [], [19], [], [], [19], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [328], [328], [], [], [], [], [], [], [], [328], [328], [], [], [], [], [], [], [328], [], [], [328], [], [], [], []]