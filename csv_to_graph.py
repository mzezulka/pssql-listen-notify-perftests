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
fig.update_layout(
    legend=dict(
        x=0,
        y=1,
        traceorder="normal",
        font=dict(
            family="sans-serif",
            size=12,
            color="black"
        ),
        bgcolor="LightSteelBlue",
        bordercolor="Black",
        borderwidth=2
    )
)
fig.update_layout(width=1300, legend=dict(x=.89, y=1), xaxis={'categoryorder':'total descending'}, xaxis_title="Performance Test name", xaxis_title_font_size=16, yaxis_title="Average Seconds per Operation", yaxis_title_font_size=16)
fig.update_yaxes(type="log")
fig.show()
