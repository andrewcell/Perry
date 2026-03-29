package client

/**
 * Represents various job classes in a game, each identified by a unique numeric ID.
 *
 * @property id The unique identifier for the job class.
 */
enum class GameJob(val id: Int) {
    /**
     * Represents the beginner job category within the game.
     *
     * This entry is used to signify the starting job or class
     * that every player begins with when they start the game.
     *
     * @param id The unique identifier associated with this job.
     */
    BEGINNER(0),

    /**
     * Represents a warrior character with specific attributes and abilities.
     *
     * This class encapsulates attributes such as health, strength, and skills
     * associated with a warrior. It provides methods to enable interactions
     * and behaviors typical for a warrior in a gameplay environment.
     *
     * @constructor Initializes the warrior with the given parameters.
     * @param health The initial health points assigned to the warrior.
     */
    WARRIOR(100),
    /**
     * Represents a fighter entity with a predefined value.
     *
     * This class provides a specific implementation or representation of
     * a combatant with an assigned value that can relate to their state,
     * point system, or other combat-based information.
     *
     * @constructor Initializes the fighter with a defined value.
     * @param value The predefined numeric value associated with the fighter.
     */
    FIGHTER(110), /**
     * Represents the CRUSADER job in the game.
     *
     * @property id The unique identifier for the CRUSADER job.
     * @constructor Initializes the CRUSADER job with its unique identifier.
     */
    CRUSADER(111), /**
     * Represents a specific job classification within the game.
     * HERO is one of the available job classifications, identified by its unique ID.
     *
     * @param id The unique identifier for the HERO classification. The default value is 112.
     */
    HERO(112),
    /**
     * Represents a type of game job with a specific page identifier.
     *
     * @property id The identifier for the page associated with this game job.
     */
    PAGE(120), /**
     * Represents the WHITEKNIGHT game job with a unique identifier.
     *
     * WHITEKNIGHT is a specialized job class within the game, identified uniquely by its ID.
     * This job class may have specific properties or methods associated with it that define
     * its behavior and interaction within the game's logic.
     *
     * @param id The unique identifier associated with the WHITEKNIGHT job.
     */
    WHITEKNIGHT(121), /**
     * Represents the PALADIN job in the game.
     *
     * PALADIN is a subclass of GameJob identified by its unique id. It may contain
     * functionality- and characteristics-specific to the PALADIN job within the game.
     *
     * Features specific to this class may include the ability to check its hierarchy
     * or compatibility with a base job through the inherited `isA` method.
     *
     * @constructor Creates an instance of a PALADIN job with its predefined id.
     */
    PALADIN(122),
    /**
     * Enum class representing various types or categories, specifically identified by the SPEARMAN type.
     *
     * @property typeCode An integer value associated with the SPEARMAN type.
     * This may represent a unique identifier or constant associated with the type.
     *
     * The SPEARMAN type might be used in contexts such as categorization of objects,
     * selection of specific functionalities, or type-based decision-making within a program.
     */
    SPEARMAN(130), /**
     * Represents the Dragon Knight job in the game.
     *
     * Dragon Knights are a type of warrior known for their high durability
     * and ability to deal with significant mêlée damage. As a subclass of
     * GameJob, this class holds a unique identifier to represent the job.
     *
     * @constructor Creates a Dragon Knight job instance with a specific ID.
     * @property id The unique identifier for the Dragon Knight job.
     */
    DRAGONKNIGHT(131), /**
     * Represents the DARKKNIGHT job within the game.
     *
     * DARKKNIGHT is a specific implementation of a GameJob with a unique identifier.
     * It corresponds to the advanced job type for the class starting as a beginner.
     *
     * @property id The unique identifier associated with the DARKKNIGHT job.
     */
    DARKKNIGHT(132),

    /**
     * A class representing a magician with specific attributes.
     *
     * @constructor Initializes the magician with a given power level.
     * @property 200 The initial power level of the magician.
     */
    MAGICIAN(200),
    /**
     * FP_WIZARD represents the job designation identifier for "Fire/Poison Wizard" in the game.
     *
     * @property id The unique identifier associated with this specific job.
     */
    FP_WIZARD(210), /**
     * Represents the Fire/Poison Mage job in the game, corresponding to a specific job ID.
     *
     * @property id The unique identifier associated with the Fire/Poison Mage job.
     * This ID is used for defining the job's specific properties and capabilities within the game.
     */
    FP_MAGE(211), /**
     * Represents the FP_ARCHMAGE job within the game, specifically ID 212.
     * This job is associated with fire and poison-based magic abilities.
     */
    FP_ARCHMAGE(212),
    /**
     * Represents the IL Wizard job in the game.
     *
     * @param id The unique identifier for the job.
     */
    IL_WIZARD(220), /**
     * Represents the IL_MAGE job in the game.
     *
     * This is a unique job classification with its own properties and behavior.
     * This identifier can be used to map specific skills, items, or abilities for the IL_MAGE job type.
     *
     * @property id The unique identifier associated with the IL_MAGE job.
     */
    IL_MAGE(221), /**
     * Represents the IL_ARCHMAGE job within the game.
     * This job is assigned a unique identifier to distinguish it from other game jobs.
     * IL_ARCHMAGE is known for its specialization in elemental magic, particularly ice and lightning-based skills.
     *
     * @property id The unique identifier of the IL_ARCHMAGE job.
     */
    IL_ARCHMAGE(222),
    /**
     * Represents the CLERIC job in the game.
     *
     * This class is a specific instance of the GameJob, identified by a unique numeric ID.
     * The ID for CLERIC is used to differentiate it from other GameJob types in the game.
     *
     * @param id The unique identifier for the CLERIC job.
     */
    CLERIC(230), /**
     * Represents the PRIEST job in the game with a unique identifier.
     * This job is likely associated with specific skills, abilities,
     * or characteristics within the game's job or class system.
     *
     * @param id The unique identifier for the PRIEST job.
     */
    PRIEST(231), /**
     * Represents the BISHOP job in the game.
     * The BISHOP job often serves as a support or magic-based class,
     * providing healing and powerful skills to help allies in battle.
     *
     * @param id The unique identifier assigned to the BISHOP job.
     */
    BISHOP(232),

    /**
     * Represents the BOWMAN class with a specified attribute value.
     * Typically used to initialize or operate on entities related to a BOWMAN archetype.
     *
     * @constructor Accepts an integer value to define the attribute parameters for the instance.
     * @param 300 Represents the initial configuration or property value associated with BOWMAN.
     */
    BOWMAN(300),
    /**
     * Represents the HUNTER job within the game characterized by a unique job identifier.
     * It is a specialization of the GameJob class and serves as one of the playable job options.
     *
     * @property id The unique identifier for the job.
     */
    HUNTER(310), /**
     * Represents the RANGER game job with its associated identifier.
     *
     * This is a specific instance of a game job which is part of the GameJob class hierarchy.
     * A game job typically determines the abilities, skills, and role of a character in gameplay.
     *
     * @param id The unique identifier for the RANGER job.
     */
    RANGER(311), /**
     * Represents the BOWMASTER job in the game with its unique identifier.
     *
     * BOWMASTER is a specialized job class that focuses on using bows and archery skills.
     * It is one of the advanced job specializations available, providing players with unique
     * gameplay opportunities and skillsets designed for ranged combat.
     *
     * @param id The unique identifier for the BOWMASTER job.
     */
    BOWMASTER(312),
    /**
     * Represents the CROSSBOWMAN job in the game with its unique identifier.
     *
     * @property id The unique identifier associated with the CROSSBOWMAN job.
     */
    CROSSBOWMAN(320), /**
     * Represents the SNIPER job in the game with a unique identifier.
     * This is a specific type of `GameJob`.
     *
     * @param id The unique identifier associated with the SNIPER job.
     */
    SNIPER(321), /**
     * Represents a specific GameJob type identified as MARKSMAN.
     * Typically associated with ranged weaponry and precision attacks in the context of the game.
     *
     * @param id The unique identifier for this job.
     */
    MARKSMAN(322),

    /**
     * The THIEF class represents a profession or role within an application,
     * potentially with specific abilities or attributes tied to its use.
     *
     * @constructor
     * Initializes a THIEF instance with a given attribute or property.
     *
     * @param value An integer parameter that may represent a key property or characteristic of a THIEF.
     */
    THIEF(400),
    /**
     * Represents the ASSASSIN job within the game.
     *
     * The ASSASSIN job is identified by its specific ID and may be used
     * in various gameplay mechanics or job classification logic.
     *
     * @param id The unique identifier for the ASSASSIN job.
     */
    ASSASSIN(410), /**
     * Represents the HERMIT job in the game, uniquely identified by an ID.
     *
     * @constructor Initializes the HERMIT job with the specified ID.
     * @param id The unique identifier for the HERMIT job.
     */
    HERMIT(411), /**
     * Represents a class named NIGHTLORD in the context of a broader system or application.
     * The specific functionality, role, or behavior of this class is determined by its implementation
     * and dependencies.
     *
     * @constructor Defines a NIGHTLORD instance with a specific value.
     * @param value An integer parameter that differentiates or identifies a specific instance.
     */
    NIGHTLORD(412),
    /**
     * Represents the BANDIT job within the `GameJob` class.
     *
     * BANDIT is a specific job with a predefined identifier that
     * is associated with certain in-game attributes or behaviors.
     *
     * @constructor Initializes the BANDIT job with its unique job identifier.
     * @property id The unique identifier for the BANDIT job.
     */
    BANDIT(420), /**
     * Represents the CHIEFBANDIT job in the game, identified by a unique job ID.
     *
     * @property id The unique identifier for the CHIEFBANDIT job.
     */
    CHIEFBANDIT(421), /**
     * The `SHADOWER` class is responsible for managing and handling shadowing operations
     * in the current context. This class may implement or encapsulate logic related
     * to data obfuscation, temporary data storage, or context-specific behavior when
     * a shadowing mechanism is required.
     *
     * @constructor The primary constructor takes an integer parameter which is used
     * to initialize the class with a predefined configuration or identifier.
     *
     * @param value An integer used to configure or initialize the SHADOWER instance.
     */
    SHADOWER(422),

    /**
     * Represents a Pirate entity with a specific bounty value.
     *
     * This class is used to encapsulate the notion of a pirate, incorporating
     * key attributes such as their bounty. Instances of this class can be used
     * within systems involving pirate-related activities, including bounty tracking
     * and pirate management systems.
     *
     * @property bounty The bounty value associated with the pirate, typically
     * used to signify their infamy or level of danger.
     */
    PIRATE(500),
    /**
     * Represents the BRAWLER job in the game.
     *
     * BRAWLER is associated with the job ID of 510. This class is an instance of the GameJob
     * hierarchy and is used to define specific properties or behaviors associated with this job type.
     */
    BRAWLER(510), /**
     * Represents the MARAUDER game job, identified by the job ID 511.
     *
     * This class defines a specific type of job within the game. It allows
     * determining whether this job is a subtype or derived type of another game job.
     *
     * @property id The unique identifier of the game job.
     */
    MARAUDER(511), /**
     * Represents the BUCCANEER job, which is identified by the job ID 512.
     * This job is part of the GameJob class hierarchy.
     *
     * @param id The unique identifier for the BUCCANEER job.
     */
    BUCCANEER(512),
    /**
     * Represents the GUNSLINGER job in the game.
     * This is a specific job type identified by the unique ID 520.
     */
    GUNSLINGER(520), /**
     * Represents the "OUTLAW" job in the game, identified by its unique ID.
     * This class is part of the predefined game job roles.
     *
     * @param id The unique identifier associated with the OUTLAW job.
     */
    OUTLAW(521), /**
     * Represents the CORSAIR job in the game.
     *
     * This enumeration entry is part of the `GameJob` class, defining a specific job
     * with its associated identifier. The identifier is used for job-related mechanics
     * and classification within the game's system.
     *
     * @param id The unique identifier associated with the job.
     */
    CORSAIR(522),

    /**
     * Represents a GM (General Manager) object with a specific identifier or value.
     * This class is initialized with an integer parameter that holds its associated value.
     *
     * @constructor
     * Initializes the GM object with the provided value.
     *
     * @param value An integer that uniquely identifies or signifies the GM object.
     */
    GM(900), /**
     * A class named SUPERGM that appears to take an integer parameter during initialization.
     *
     * @constructor Creates an instance of SUPERGM initialized with a specified integer value.
     * @param value An integer value passed to the class during initialization.
     */
    SUPERGM(910);

    /**
     * Checks if the current job is of type `baseJob` or a more advanced version of it.
     *
     * @param baseJob The base job to compare against.
     * @return `true` if the current job is the same as or a more advanced version of `baseJob`; `false` otherwise.
     */
    fun isA(baseJob: GameJob) = id >= baseJob.id && id / 100 == baseJob.id / 100

    /**
     * Companion object for utility functions related to game jobs.
     */
    companion object {
        /**
         * Retrieves a GameJob entry by its unique identifier.
         *
         * @param id The unique identifier of the GameJob to be retrieved.
         * @return The GameJob entry with the matching id, or null if no match is found.
         */
        fun getById(id: Int) = GameJob.entries.find { it.id == id }

        /**
         * Decodes a given 5-byte encoded integer into a corresponding character type.
         *
         * @param encoded The integer value representing the 5-byte encoded character type.
         *                Possible values:
         *                - 2: WARRIOR
         *                - 4: MAGICIAN
         *                - 8: BOWMAN
         *                - 16: THIEF
         *                - 32: PIRATE
         *                Any other value will return BEGINNER as the default.
         * @return The character type based on the input-encoded value.
         */
        fun getBy5ByteEncoding(encoded: Int) = when (encoded) {
            2 -> WARRIOR
            4 -> MAGICIAN
            8 -> BOWMAN
            16 -> THIEF
            32 -> PIRATE
            else -> BEGINNER
        }
    }
}