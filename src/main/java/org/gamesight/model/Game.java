package org.gamesight.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "games")
public class Game implements Serializable {

	/*
	A Game entity holds the data describing a game and holds references to all
	 Players of this game.
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	String name;

	String category;

	/*
		Configure data relationship. A Player may have many Games they are playing.
		The same Game may also be used by multiple Players. We have a many-to-many
		relationship.
	 */
	@JsonBackReference
	@ManyToMany(fetch = FetchType.LAZY,
			cascade = {
					CascadeType.PERSIST,
					CascadeType.MERGE },
			mappedBy = "games")
	private Set<Player> players = new HashSet<>();


	public Game() {
	}

	public Game(String name, String category) {
		this.name = name;
		this.category = category;
	}


	public String getName() {
		return name;
	}


	public Set<Player> getPlayers() {

		return players;
	}

	public String getCategory() {

		return category;
	}

	public void setCategory(String category) {

		this.category = category;
	}
}
