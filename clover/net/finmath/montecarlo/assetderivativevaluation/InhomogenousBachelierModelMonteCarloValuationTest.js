var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":543,"id":41376,"methods":[{"el":107,"sc":2,"sl":64},{"el":111,"sc":2,"sl":109},{"el":139,"sc":2,"sl":113},{"el":160,"sc":2,"sl":141},{"el":210,"sc":2,"sl":165},{"el":240,"sc":2,"sl":217},{"el":266,"sc":2,"sl":245},{"el":333,"sc":2,"sl":280},{"el":364,"sc":5,"sl":354},{"el":378,"sc":2,"sl":340},{"el":448,"sc":2,"sl":383},{"el":521,"sc":2,"sl":453},{"el":528,"sc":2,"sl":526},{"el":535,"sc":2,"sl":533},{"el":542,"sc":2,"sl":540}],"name":"InhomogenousBachelierModelMonteCarloValuationTest","sl":40}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_212":{"methods":[{"sl":141},{"sl":245}],"name":"testModelRandomVariable","pass":true,"statements":[{"sl":146},{"sl":148},{"sl":151},{"sl":154},{"sl":156},{"sl":159},{"sl":250},{"sl":252},{"sl":254},{"sl":255},{"sl":256},{"sl":257},{"sl":260},{"sl":261},{"sl":263},{"sl":265}]},"test_240":{"methods":[{"sl":141},{"sl":217}],"name":"testModelProperties","pass":true,"statements":[{"sl":146},{"sl":148},{"sl":151},{"sl":154},{"sl":156},{"sl":159},{"sl":222},{"sl":224},{"sl":226},{"sl":227},{"sl":228},{"sl":229},{"sl":231},{"sl":232},{"sl":233},{"sl":235},{"sl":236},{"sl":237},{"sl":239}]},"test_314":{"methods":[{"sl":141},{"sl":453},{"sl":526},{"sl":533},{"sl":540}],"name":"testEuropeanCallVega","pass":true,"statements":[{"sl":146},{"sl":148},{"sl":151},{"sl":154},{"sl":156},{"sl":159},{"sl":459},{"sl":462},{"sl":463},{"sl":464},{"sl":466},{"sl":467},{"sl":468},{"sl":471},{"sl":472},{"sl":474},{"sl":475},{"sl":476},{"sl":477},{"sl":480},{"sl":483},{"sl":485},{"sl":486},{"sl":488},{"sl":490},{"sl":491},{"sl":492},{"sl":495},{"sl":497},{"sl":498},{"sl":500},{"sl":508},{"sl":511},{"sl":518},{"sl":520},{"sl":527},{"sl":534},{"sl":541}]},"test_455":{"methods":[{"sl":141},{"sl":383},{"sl":526},{"sl":533},{"sl":540}],"name":"testEuropeanCallDelta","pass":true,"statements":[{"sl":146},{"sl":148},{"sl":151},{"sl":154},{"sl":156},{"sl":159},{"sl":389},{"sl":392},{"sl":393},{"sl":394},{"sl":396},{"sl":397},{"sl":398},{"sl":401},{"sl":402},{"sl":404},{"sl":405},{"sl":406},{"sl":407},{"sl":410},{"sl":413},{"sl":415},{"sl":416},{"sl":418},{"sl":420},{"sl":421},{"sl":422},{"sl":425},{"sl":428},{"sl":435},{"sl":438},{"sl":445},{"sl":447},{"sl":527},{"sl":534},{"sl":541}]},"test_89":{"methods":[{"sl":141},{"sl":165},{"sl":526},{"sl":533},{"sl":540}],"name":"testEuropeanCall","pass":true,"statements":[{"sl":146},{"sl":148},{"sl":151},{"sl":154},{"sl":156},{"sl":159},{"sl":171},{"sl":174},{"sl":175},{"sl":176},{"sl":179},{"sl":180},{"sl":182},{"sl":183},{"sl":184},{"sl":186},{"sl":188},{"sl":189},{"sl":191},{"sl":194},{"sl":196},{"sl":199},{"sl":202},{"sl":207},{"sl":209},{"sl":527},{"sl":534},{"sl":541}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [314, 89, 212, 455, 240], [], [], [], [], [314, 89, 212, 455, 240], [], [314, 89, 212, 455, 240], [], [], [314, 89, 212, 455, 240], [], [], [314, 89, 212, 455, 240], [], [314, 89, 212, 455, 240], [], [], [314, 89, 212, 455, 240], [], [], [], [], [], [89], [], [], [], [], [], [89], [], [], [89], [89], [89], [], [], [89], [89], [], [89], [89], [89], [], [89], [], [89], [89], [], [89], [], [], [89], [], [89], [], [], [89], [], [], [89], [], [], [], [], [89], [], [89], [], [], [], [], [], [], [], [240], [], [], [], [], [240], [], [240], [], [240], [240], [240], [240], [], [240], [240], [240], [], [240], [240], [240], [], [240], [], [], [], [], [], [212], [], [], [], [], [212], [], [212], [], [212], [212], [212], [212], [], [], [212], [212], [], [212], [], [212], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [455], [], [], [], [], [], [455], [], [], [455], [455], [455], [], [455], [455], [455], [], [], [455], [455], [], [455], [455], [455], [455], [], [], [455], [], [], [455], [], [455], [455], [], [455], [], [455], [455], [455], [], [], [455], [], [], [455], [], [], [], [], [], [], [455], [], [], [455], [], [], [], [], [], [], [455], [], [455], [], [], [], [], [], [314], [], [], [], [], [], [314], [], [], [314], [314], [314], [], [314], [314], [314], [], [], [314], [314], [], [314], [314], [314], [314], [], [], [314], [], [], [314], [], [314], [314], [], [314], [], [314], [314], [314], [], [], [314], [], [314], [314], [], [314], [], [], [], [], [], [], [], [314], [], [], [314], [], [], [], [], [], [], [314], [], [314], [], [], [], [], [], [314, 89, 455], [314, 89, 455], [], [], [], [], [], [314, 89, 455], [314, 89, 455], [], [], [], [], [], [314, 89, 455], [314, 89, 455], [], []]