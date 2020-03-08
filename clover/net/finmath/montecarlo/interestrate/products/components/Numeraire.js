var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":50,"id":26782,"methods":[{"el":27,"sc":2,"sl":25},{"el":32,"sc":2,"sl":29},{"el":49,"sc":2,"sl":44}],"name":"Numeraire","sl":18}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_295":{"methods":[{"sl":25},{"sl":44}],"name":"testPutOnMoneyMarketAccount","pass":true,"statements":[{"sl":26},{"sl":47}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [295], [295], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [295], [], [], [295], [], [], []]