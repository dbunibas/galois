#!/usr/bin/env python3
"""
Make sure to run:
python src/cli/cli_main.py pz reg --path floq/core/src/test/resources/rag-premierleague/documents --name floq-pl-rag
(or pz reg ...)
"""

import argparse
import json
import os
import sys
import time

import pandas as pd
from tabulate import tabulate

import palimpzest as pz
from palimpzest.dataclasses import ExecutionStats
from palimpzest.elements.groupbysig import GroupBySig
from palimpzest.policy import MaxQualityAtFixedCost
from palimpzest.utils import token_tracker, udfs
from palimpzest.utils.env_helpers import load_env

load_env()

class FootballMatch(pz.TextFile):
    """Represents a football match, typically recorded in a structured format or report."""

    date = pz.StringField(
        desc="date",
        required=True,
    )
    home_team = pz.StringField(
        desc="home_team",
        required=True,
    )
    away_team = pz.StringField(
        desc="away_team",
        required=True,
    )
    home_goals = pz.NumericField(
        desc="home_goals",
        required=True,
    )
    away_goals = pz.NumericField(
        desc="away_goals",
        required=True,
    )
    player_of_the_match = pz.StringField(
        desc="player_of_the_match",
        required=True,
    )
    player_of_the_match_team = pz.StringField(
        desc="player_of_the_match_team",
        required=True,
    )

class FootballPlayerOfTheMatchName(pz.TextFile):
    """FootballPlayerOfTheMatchName"""

    player_of_the_match = pz.StringField(
        desc="player_of_the_match",
        required=True,
    )

class FootballPlayerOfTheMatchWithTeam(pz.TextFile):
    """FootballPlayerOfTheMatchWithTeam"""

    player_of_the_match = pz.StringField(
        desc="player_of_the_match",
        required=True,
    )
    player_of_the_match_team = pz.StringField(
        desc="player_of_the_match_team",
        required=True,
    )

class ArsenalFootballMatchOpponent(pz.TextFile):
    """ArsenalFootballMatchOpponent"""

    match_date_year = pz.NumericField(
        desc="match_date_year",
        required=True,
    )
    match_date_month = pz.NumericField(
        desc="match_date_month",
        required=True,
    )
    match_date_day = pz.NumericField(
        desc="match_date_day",
        required=True,
    )
    opponent_team = pz.StringField(
        desc="opponent_team",
        required=True,
    )

class FootballMatchResult(pz.TextFile):
    """FootballMatchResult"""

    home_team = pz.StringField(
        desc="home_team",
        required=True,
    )
    away_team = pz.StringField(
        desc="away_team",
        required=True,
    )
    home_goals = pz.NumericField(
        desc="home_goals",
        required=True,
    )
    away_goals = pz.NumericField(
        desc="away_goals",
        required=True,
    )


def print_table(records, policy, variant: int, cols=None, gradio=False, plan_str=None):
    records = [{key: record[key] for key in record.get_fields()} for record in records]
    records_df = pd.DataFrame(records)
    cols = [col for col in cols if col in records_df.columns]
    filtered_df = records_df[cols]
    print(tabulate(filtered_df, headers='keys', tablefmt='grid'))
    if variant == 2:
        with open(f'exp-results/{policy}/{EXP_NAME}-Q{variant}.csv', "w") as f:
            f.write(f'count\n{len(records)}')
        pass
    else:
        filtered_df.to_csv(f'exp-results/{policy}/{EXP_NAME}-Q{variant}.csv', sep=';', encoding='utf-8', index=False, header=True)

def get_exp(variant: int):

    if variant == 1:
        # .querySql("select m.opponent_team, m.match_date_year, m.match_date_month, m.match_date_day from premier_league_2024_2025_arsenal_matches m")
        # .prompt("List the date (year, month, day) and opponent for each of Arsenal's 2024-25 Premier League season matches")
        football_matches_md = pz.Dataset("floq-pl-rag", schema=FootballMatch)
        arsenal_football_matches_md = football_matches_md.filter("The record contains information about an Arsenal match")
        arsenal_opponent_football_matches_md = arsenal_football_matches_md.convert(ArsenalFootballMatchOpponent)
        return arsenal_opponent_football_matches_md
    
    if variant == 2:
        # .querySql("select count(*) from premier_league_2024_2025_arsenal_matches m where m.match_date_month = 8")
        # .prompt("Count the number of Arsenal matches in the Premier League for the month of August during the 2024-2025 season")
        football_matches_md = pz.Dataset("floq-pl-rag", schema=FootballMatch)
        arsenal_football_matches_md = football_matches_md.filter("The record contains information about an Arsenal match")
        arsenal_football_matches_md = arsenal_football_matches_md.filter("The match was played in August")
        # return arsenal_football_matches_md.groupby(GroupBySig(group_by_fields= ["date"], agg_funcs= ["count"], agg_fields= ["date"]))
        return arsenal_football_matches_md

    if variant == 3:
        # .querySql("select m.player_of_the_match from premier_league_2024_2025_match_result m")
        # .prompt("List the names of the players who were awarded 'Player of the Match' during the 2024-2025 Premier League season")
        return pz.Dataset("floq-pl-rag", schema=FootballPlayerOfTheMatchName)

    if variant == 4:
        # .querySql("select m.player_of_the_match from premier_league_2024_2025_match_result m where m.player_of_the_match_team = 'Manchester United'")
        # .prompt("List the names of the players who were awarded 'Player of the Match' while playing for Manchester United during the 2024-2025 Premier League season")
        players_of_the_matches = pz.Dataset("floq-pl-rag", schema=FootballPlayerOfTheMatchWithTeam)
        # players_of_the_matches = players_of_the_matches.filter("The player of the match teams is Manchester United")
        players_of_the_matches = players_of_the_matches.filter("player_of_the_match_team = 'Manchester United'")
        # players_of_the_matches = players_of_the_matches.convert(FootballPlayerOfTheMatchName)
        return players_of_the_matches

    if variant == 5:
        # .querySql("select home_team,away_team,home_goals,away_goals from premier_league_2024_2025_match_result")
        # .prompt("Retrieve the match details from the 2024-2025 Premier League season, including the home team, away team, and the number of goals scored by both teams")
        return pz.Dataset("floq-pl-rag", schema=FootballMatchResult)


    raise ValueError("Unknown variant")


if __name__ == "__main__":

    EXP_NAME = 'FLOQ-PL-RAG'

    for variant in range(5, 6):
        print(variant)

        startTime = time.time()
        # #Sentinel -> sampling
        # engine = pz.SequentialSingleThreadSentinelExecution
        # engine = pz.PipelinedSingleThreadSentinelExecution
        # engine = pz.PipelinedParallelSentinelExecution
        # #NoSentinel -> no sampling
        engine = pz.SequentialSingleThreadNoSentinelExecution
        # engine = pz.PipelinedSingleThreadNoSentinelExecution
        # engine = pz.PipelinedParallelNoSentinelExecution

        # no_cache:
        # pz.DataDirectory().clear_cache(keep_registry=True)

        # policy = pz.MinCost()
        policy = MaxQualityAtFixedCost(max_cost=1.0) #Suggerito da Gerardo
        # policy = pz.MaxQuality()
        print(f'Policy: {policy}')

        result = get_exp(variant=variant)
        columns = [item for item in result.schema.field_names() if item not in ['contents', 'filename']]
        if variant == 4:
            columns = ["player_of_the_match"]

        tables, stats = pz.Execute(
            result,
            policy=policy,
            nocache=True,
            allow_code_synth=False,
            allow_token_reduction=False,
            execution_engine=engine,
            verbose=True
        )

        stats: ExecutionStats = stats
        stats.total_used_tokens = token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()

        print(f'##### {EXP_NAME}-Q{variant} ####')
        print(f'Stats - total_used_tokens: {token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()}')
        print(f'Stats - total_execution_cost: {stats.total_execution_cost}')
        print(f'Stats - total_execution_time: {stats.total_execution_time}')
        print('Result')
        print_table(tables, policy=policy, variant=variant, cols=columns)

        endTime = time.time()
        print("Elapsed time:", endTime - startTime)
        with open(f'exp-results/{policy}/{EXP_NAME}-Q{variant}.json', "w") as f:
            json.dump(stats.to_json(), f)

        token_tracker.reset()
        print(f'Stats - total_used_tokens: {token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()}')