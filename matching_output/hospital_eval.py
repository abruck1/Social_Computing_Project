import pandas as pd
import numpy as np
import pprint

file_names = ["export_hos100_loc10_res200_coup50_hospitals.csv",
"export_hos300_loc50_res500_coup100_hospitals.csv",
"export_hos50_loc50_res100_coup50_hospitals.csv",
"export_hos50_loc50_res150_coup20_hospitals.csv",
"export_hos5_loc2_res16_coup3_hospitals.csv"]

def get_happiness(hospital_prefs, assignment_ranks, capacities):
    happiness = []
    for i, ranks in enumerate(assignment_ranks):
        int_ranks = [int(rank) if int(rank) > -1 else len(hospital_prefs) for rank in ranks]
        while(len(int_ranks) < capacities[i]):
            int_ranks.append(len(hospital_prefs))
        happiness.append(sum(int_ranks)/capacities[i])
    return happiness


for i, file_name in enumerate(file_names):
    df = pd.read_csv(file_name)
    assignment_ranks = list(df[' assignmentRanks'].str.split(' '))
    assignment_ranks = [list(filter(None, sublist)) for sublist in assignment_ranks]
    preferences = list(df[' preferences'].str.split(' '))
    preferences = [list(filter(None, sublist)) for sublist in preferences]
    capacities = list(df[' capacity'])
    happiness = get_happiness(preferences, assignment_ranks, capacities)
    print(file_name + ": ")
    print("Average Happiness: " + str(np.average(happiness)))
    print("Standard Deviation: " + str(np.std(happiness)))
    # print("Max Happiness: " + str(np.min(happiness)))
    # print("Min Happiness: " + str(np.max(happiness)))
    percentages = list(df[' filledPercentage'])
    print("average filled percentage: " + str(sum(percentages)/len(percentages)))
    print()
