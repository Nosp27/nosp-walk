import json

import flask
from flask import request
from datetime import datetime
import psycopg2
from flask import make_response
from werkzeug import exceptions
import waitress


app = flask.Flask(__name__)


def query(sql, *args):
    with psycopg2.connect("dbname=nosp_walk user=postgres") as conn:
        with conn.cursor() as cursor:
            cursor.execute(sql, *args)
            return list(cursor)


@app.route("/")
def main_page():
    return {"page": "main"}


@app.route("/sign_in", methods=["POST"])
def sign_in():
    json_data = json.loads(request.data)
    key_hash = json_data["key"]
    try:
        user = query(
                    f"select _id from users where key_hash = '{key_hash}'",
        )[0][0]
    except Exception as e:
        raise exceptions.Forbidden(e)

    if not user:
        raise exceptions.Forbidden()

    resp = make_response({"access": "granted"})
    resp.set_cookie("user", str(user))
    return resp


@app.route("/turn")
def turn():
    user = request.cookies.get("user")
    if not user:
        print("no user")
        return {"error": "cannot resolve user"}, 400
    walking_user_id = query(
    """
    select
        user_id,
        count(*) as walk_count
    from walks
    group by user_id
    order by walk_count
    """)[0][0]

    if str(walking_user_id) == user:
        return {"turn": "you"}
    return {"turn": "other"}


@app.route("/history")
def get_history():
    req_data = request.args
    from_date, to_date = (req_data["from"], req_data["to"])
    walks = query(
        f"""
    select * from walks
    where walked_at between $1 and $2 
    """,
        datetime.fromtimestamp(from_date),
        datetime.fromtimestamp(to_date),
    )

    walks = [
        {"date": w["date"], "time": w["time"], "walker": w["walker"]} for w in walks
    ]
    return walks


@app.route("/walk", methods=["POST"])
def walk():
    json_data = json.loads(request.data)

    walk_timestamp = json_data["timestamp"]
    walker = json_data["walker"]
    assert walker in ("P", "M")

    if request.cookies.get("user") != walker:
        raise exceptions.Forbidden()

    query(
        f"""
    insert into walks (walker, walked_at) values ($1, $2)
    """,
        datetime.fromtimestamp(walk_timestamp),
        walker,
    )

    return {"status": "walk_registered"}
