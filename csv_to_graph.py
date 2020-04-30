#!/usr/bin/env python3
# required dependencies: numpy, plotly, pandas

import pandas as pd
import plotly.graph_objects as go
import sys
import re
import math
import numpy as np

fig = go.Figure()

for f in sys.argv[1:]:
    x=[]
    y=[]
    params=[]
    for line in pd.read_csv(f).filter(items=["Benchmark", "Score", "Param: noProcesses", "Param: numInserts"]).groupby(["Benchmark", "Score"]):
        (name, score, firstParam, secondParam) = line[0] + (line[1]["Param: noProcesses"].__iter__().__next__(), line[1]["Param: numInserts"].__iter__().__next__())
        x.append('/'.join(name.split('.')[-1:]))
        y.append(score)
        if not math.isnan(firstParam):
            params.append(firstParam)
        elif not math.isnan(secondParam):
            params.append(secondParam)
        else:
            params.append("")
    fig.add_trace(go.Bar(x=x, y=y, text=params, textposition='auto',
    #            marker_color='crimson',
                name=f.split(".")[0]))

fig.update_layout(barmode='stack', xaxis={'categoryorder':'total descending'})
fig.show()
